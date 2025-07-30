/*
 * Copyright (C) 2024-2025 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.me.service;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.events.GridBootingStatusChange;
import appeng.api.networking.events.GridPowerStatusChange;
import appeng.api.networking.ticking.TickRateModulation;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import stone.mae2.MAE2;
import stone.mae2.api.Tickable;
import stone.mae2.api.Tickable.TickingEntry;
import stone.mae2.parts.p2p.multi.MultiP2PTunnel;

import java.util.PriorityQueue;
import java.util.Random;

/**
 * 
 */
public class MultiP2PService implements IGridService, IGridServiceProvider {
  public static final String TAG_NAME = "mae2:multi_p2p";

  static {
    GridHelper
      .addGridServiceEventHandler(GridBootingStatusChange.class,
        MultiP2PService.class, (service, evt) -> {
          if (!evt.isBooting()) {
            service.wakeInputTunnels();
          }
        });
    GridHelper
      .addGridServiceEventHandler(GridPowerStatusChange.class,
        MultiP2PService.class, (service, evt) -> {
          service.wakeInputTunnels();
        });
  }

  public static MultiP2PService get(IGrid grid) {
    return grid.getService(MultiP2PService.class);
  }

  private final IGrid myGrid;
  // terrible nested maps, but performance or something
  // 0 initial size is to save on a few bytes of memory per subnet
  /**
   * This a map with keys of attunement ids (given out by this class) mapping to
   * maps with keys of shorts mapping to actual {@link MultiP2PTunnel} objects.
   */
  private final Reference2ReferenceMap<Class<? extends MultiP2PTunnel<?, ?, ?>>, Short2ReferenceMap<? extends MultiP2PTunnel<?, ?, ?>>> attunement2frequency2tunnelMap = new Reference2ReferenceOpenHashMap<>(
    0);
  private final Random frequencyGenerator;

  private final PriorityQueue<TickingEntry> tickingQueue = new PriorityQueue<>(
    1);
  private boolean tickable = false;
  private long currentTick = 0;

  public int[] transferredAmps;

  public MultiP2PService(IGrid g) {
    this.myGrid = g;
    // add one to hopefully prevent both P2P services from making the same
    // frequencies
    this.frequencyGenerator = new Random(g.hashCode() + 1);
    if (MAE2.CONFIG.parts().isEUP2PNerfed())
      transferredAmps = new int[GTValues.TIER_COUNT];
  }

  @Override
  public void onServerStartTick() {
    this.currentTick++;
  }

  @Override
  public void onServerEndTick() {
    // short circuit since most networks won't have ticking tunnels
    if (this.tickable) {
      for (int i = 0; i < transferredAmps.length; i++) {
        int amps = transferredAmps[i];
        if (amps > 0) {
          var tax = PowerUnits.FE
            .convertTo(PowerUnits.AE, amps * GTValues.V[i] * getNerfTax(i, amps)
              * FeCompat.ratio(false));
          this.myGrid
            .getEnergyService()
            .extractAEPower(tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
          transferredAmps[i] = 0;
        }
      }
      while (this.tickingQueue.peek().getNextTick() <= this.currentTick) {
        TickingEntry entry = this.tickingQueue.poll();
        // MAE2.LOGGER.info("Ticking tunnel {} on tick {}", entry,
        // this.currentTick);
        TickRateModulation mod = entry.getTickable().tick();
        if (mod != TickRateModulation.SLEEP) {
          entry.update(mod);
          // MAE2.LOGGER.info("Next scheduled tick {}", entry.getNextTick());
          this.tickingQueue.add(entry);
        } else {
          // TODO add support for waking up (can't wake up)
        }
      }
    }
  }

  public static double getNerfTax(long voltageTier, long amperage) {
    final double costFactor;
    costFactor = MAE2.CONFIG.parts().EUP2PNerfFactor();
    if (costFactor <= 0) {
      return 0;
    }
    return (voltageTier + 2) * Math.log1p(amperage) * costFactor;
  }

  public void setTickable() {
    this.tickable = true;
  }

  public void wakeInputTunnels() {
    var tm = this.myGrid.getTickManager();
  }

  public MultiP2PTunnel<?, ?, ?> getTunnel(MultiP2PTunnel.Part part) {
    Short2ReferenceMap<MultiP2PTunnel> freq2tunnelMap = this.attunement2frequency2tunnelMap
      .computeIfAbsent(part.getTunnelClass(),
        ($) -> new Short2ReferenceOpenHashMap<>());
    // cast is okay because the types will always end up matching
    return freq2tunnelMap.computeIfAbsent(part.getFrequency(), freq -> {
      var tunnel = part.createTunnel(freq);
      if (tunnel instanceof Tickable tickable) {
        // MAE2.LOGGER.info("Creating new tickable tunnel {}", tickable);
        addTickable(tickable);
      }
      return tunnel;
    });
  }

  public void addTickable(TickingEntry entry) {
    this.tickable = true;
    this.tickingQueue.add(entry);
  }

  public void addTickable(Tickable tickable) {
    addTickable(new TickingEntry(tickable.getTickingRequest(), tickable,
      this.currentTick));
  }

  @Override
  public void removeNode(IGridNode node) {
    // needs a raw reference because I can't figure out how else to get the
    // generics
    // to work
    // okay here because the part should always reference the right type of
    // tunnel
    if (node.getOwner() instanceof MultiP2PTunnel.Part part) {
      // this will leave an empty MultiTunnel in memory, but the player might
      // make
      // parts with the freq again, otherwise it'll get wiped on world restart
      // or
      // something
      MultiP2PTunnel tunnel = getTunnel(part);
      tunnel.removeTunnel(part);
    }
  }

  @Override
  public void addNode(IGridNode node, @Nullable CompoundTag savedData) {
    // raw for same reasons in removeNode
    if (node.getOwner() instanceof MultiP2PTunnel.Part part) {
      MultiP2PTunnel tunnel = getTunnel(part);
      tunnel
        .addTunnel(part, savedData != null ? savedData.get(TAG_NAME) : null);
    }
  }

  @Override
  public void saveNodeData(IGridNode node, CompoundTag savedData) {
    // TODO Auto-generated method stub
    if (node.getOwner() instanceof MultiP2PTunnel.Part part) {
      MultiP2PTunnel tunnel = getTunnel(part);
      Tag toSave = tunnel.saveNodeData(part);
      if (toSave != null) {
        savedData.put(TAG_NAME, toSave);
      }
    }
  }

  public short newFrequency(Class<? extends MultiP2PTunnel<?, ?, ?>> tunnel) {
    short newFrequency;
    int cycles = 0;

    var tunnels = this.attunement2frequency2tunnelMap.get(tunnel);
    do {
      newFrequency = (short) this.frequencyGenerator.nextInt(1 << 16);
      cycles++;
    } while (newFrequency == 0 || tunnels.containsKey(newFrequency));

    if (cycles > 25) {
      MAE2.LOGGER
        .debug("Generating a new MultiP2P frequency '%1$d' took %2$d cycles",
          newFrequency, cycles);
    }
    return newFrequency;
  }
}

/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.parts.p2p;

import appeng.api.config.PowerUnits;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.Direction;

import stone.mae2.MAE2;
import stone.mae2.me.service.MultiP2PService;

import java.util.List;

public class EUP2PTunnelPart
  extends CapabilityP2PTunnelPart<EUP2PTunnelPart, IEnergyContainer> {
  private static final P2PModels MODELS = new P2PModels(
    MAE2.toKey("part/p2p/p2p_tunnel_eu"));
  private static final IEnergyContainer EMPTY_HANDLER = new EmptyHandler();

  public EUP2PTunnelPart(IPartItem<?> partItem) {
    super(partItem, GTCapability.CAPABILITY_ENERGY_CONTAINER);
    if (MAE2.CONFIG.parts().isEUP2PNerfed()) {
      this
        .getMainNode()
        .setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.COMPRESSED_CHANNEL);
    }
    this.inputHandler = new InputHandler();
    this.outputHandler = new OutputHandler();
    this.emptyHandler = EMPTY_HANDLER;
  }

  @Override
  protected void onMainNodeStateChanged(State reason) {
    if (reason == State.GRID_BOOT) {
      if (this.getMainNode().isActive())
        this
          .getMainNode()
          .getGrid()
          .getService(MultiP2PService.class)
          .setTickable();
    }
  }

  @PartModels
  public static List<IPartModel> getModels() { return MODELS.getModels(); }

  @Override
  public IPartModel getStaticModels() {
    return MODELS.getModel(this.isPowered(), this.isActive());
  }

  public class InputHandler implements IEnergyContainer {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage,
      long amperage) {
      long toSend = amperage;
      int total = 0;
      int outputs = 0;
      for (EUP2PTunnelPart target : EUP2PTunnelPart.this.getOutputs()) {
        try (CapabilityGuard guard = target.getAdjacentCapability()) {
          outputs++;
          final long received = guard
            .get()
            .acceptEnergyFromNetwork(target.getSide().getOpposite(), voltage,
              toSend);

          toSend -= received;
          total += received;
          if (toSend <= 0) {
            break;
          }
        }
      }
      if (total > 0) {
        if (MAE2.CONFIG.parts().isEUP2PNerfed()) {
          int tier = (int) Math.round(Math.log1p(voltage / 8) / Math.log(4));
          EUP2PTunnelPart.this
            .getGridNode()
            .getGrid()
            .getService(MultiP2PService.class).transferredAmps[tier] += total;
        } else
          EUP2PTunnelPart.this
            .deductEnergyCost(total * FeCompat.ratio(false), PowerUnits.FE);
      }
      return total;
    }

    @Override
    public boolean inputsEnergy(Direction side) {
      return EUP2PTunnelPart.this.getSide() == side;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
      return 0;
    }

    @Override
    public long getEnergyStored() {
      long total = 0;

      for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
        try (CapabilityGuard guard = part.getAdjacentCapability()) {
          try {
            total = Math.addExact(total, guard.get().getEnergyStored());
          } catch (ArithmeticException e) {
            // combined output's storing more than a long's worth of power,
            // return 0
            // instead, because otherwise it'll look like it'll look full, ie
            // storing a long
            // of power, has a max capacity of a long -> full storage
            return 0;
          }
        }
      }

      return total;
    }

    @Override
    public long getEnergyCapacity() {
      long total = 0;

      for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
        try (CapabilityGuard guard = part.getAdjacentCapability()) {
          try {
            total = Math.addExact(total, guard.get().getEnergyCapacity());
          } catch (ArithmeticException e) {
            // combined output's capacity is more than a long's worth of power,
            // return max
            // long
            // instead, because otherwise it'll look like it'll look full, ie
            // storing a long
            // of power, has a max capacity of a long -> full storage
            return Long.MAX_VALUE;
          }
        }
      }

      return total;
    }

    @Override
    public long getInputAmperage() {
      long total = 0;

      for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
        try (CapabilityGuard guard = part.getAdjacentCapability()) {
          try {
            total = Math.addExact(total, guard.get().getInputAmperage());
          } catch (ArithmeticException e) {
            // combined outputs want more than a long's worth of power, return
            // max long
            return Long.MAX_VALUE;
          }
        }
      }

      return total;
    }

    @Override
    public long getInputVoltage() {
      long maxVoltage = 0;

      for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
        try (CapabilityGuard guard = part.getAdjacentCapability()) {
          maxVoltage = Math.max(maxVoltage, guard.get().getInputVoltage());
        }
      }

      return maxVoltage;
    }
  }

  public class OutputHandler implements IEnergyContainer {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage,
      long amperage) {
      return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) {
      return false;
    }

    @Override
    public boolean outputsEnergy(Direction side) {
      return EUP2PTunnelPart.this.getSide() == side;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
      return 0;
    }

    @Override
    public long getEnergyStored() {
      EUP2PTunnelPart input = EUP2PTunnelPart.this.getInput();
      if (input != null)
        try (CapabilityGuard guard = input.getAdjacentCapability()) {
          return guard.get().getEnergyStored();
        }
      else
        return 0;
    }

    @Override
    public long getEnergyCapacity() {
      EUP2PTunnelPart input = EUP2PTunnelPart.this.getInput();
      if (input != null) {
        try (CapabilityGuard guard = input.getAdjacentCapability()) {
          return guard.get().getEnergyCapacity();
        }
      } else
        return 0;
    }

    @Override
    public long getOutputAmperage() {
      EUP2PTunnelPart input = EUP2PTunnelPart.this.getInput();
      if (input != null) {
        try (CapabilityGuard guard = input.getAdjacentCapability()) {
          return guard.get().getOutputAmperage();
        }
      } else
        return 0;
    }

    @Override
    public long getOutputVoltage() {
      EUP2PTunnelPart input = EUP2PTunnelPart.this.getInput();
      if (input != null) {
        try (CapabilityGuard guard = input.getAdjacentCapability()) {
          return guard.get().getOutputVoltage();
        }
      } else
        return 0;
    }

    @Override
    public long getInputAmperage() { return 0; }

    @Override
    public long getInputVoltage() { return 0; }
  }

  private static class EmptyHandler implements IEnergyContainer {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage,
      long amperage) {
      return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) {
      return false;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
      return 0;
    }

    @Override
    public long getEnergyStored() { return 0; }

    @Override
    public long getEnergyCapacity() { return 0; }

    @Override
    public long getInputAmperage() { return 0; }

    @Override
    public long getInputVoltage() { return 0; }
  }
}

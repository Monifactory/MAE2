/*
 * Copyright (C) 2024-2025 AE2 Enthusiast
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
package stone.mae2.me.service;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.events.GridBootingStatusChange;
import appeng.api.networking.events.GridPowerStatusChange;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenHashMap;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;

import org.jetbrains.annotations.Nullable;

import stone.mae2.MAE2;
import stone.mae2.parts.p2p.multi.CapabilityMultiP2PPart;
import stone.mae2.parts.p2p.multi.MultiP2PTunnel;
import stone.mae2.parts.p2p.multi.MultiP2PTunnelPart;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 
 */
public class MultiP2PService implements IGridService, IGridServiceProvider {
    // atomic because mods load asynchronously/do whatever they want
    // These ids *have* to be unique per tunnel type.
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
    static
    {
        GridHelper.addGridServiceEventHandler(GridBootingStatusChange.class, MultiP2PService.class,
                                              (service, evt) ->
                                              {
                                                  if (!evt.isBooting())
                                                      {
                                                          service.wakeInputTunnels();
                                                      }
                                              });
        GridHelper.addGridServiceEventHandler(GridPowerStatusChange.class, MultiP2PService.class,
                                              (service, evt) ->
                                              {
                                                  service.wakeInputTunnels();
                                              });
    }

    private final IGrid myGrid;
    // terrible nested maps, but performance or something
    // 0 initial size is to save on a few bytes of memory per subnet
    /**
     * This a map with keys of attunement ids (given out by this class) mapping to maps with keys of shorts mapping to actual {@link MultiP2PTunnel} objects.
     */
    private final Short2ReferenceMap<Short2ReferenceMap<MultiP2PTunnel<?>>> attunement2frequency2tunnelMap = new Short2ReferenceOpenHashMap<>(0);
    private final Random frequencyGenerator;

    public MultiP2PService(IGrid g) {
        this.myGrid = g;
        // add one to hopefully prevent both P2P services from making the same frequencies
        this.frequencyGenerator = new Random(g.hashCode() + 1);
    }

    public void wakeInputTunnels() {
        var tm = this.myGrid.getTickManager();
    }

    public MultiP2PTunnel<?> getTunnel(short tunnelID, short freq, final Supplier<MultiP2PTunnel<?>> tunnelSupplier) {
        Short2ReferenceMap<MultiP2PTunnel<?>> freq2tunnelMap = this.attunement2frequency2tunnelMap.computeIfAbsent(tunnelID, ($) -> new Short2ReferenceOpenHashMap<>());
        MultiP2PTunnel<?> tunnel = freq2tunnelMap.computeIfAbsent(freq, ($) -> tunnelSupplier.get());
        return tunnel;
    }

    public MultiP2PTunnel<?> getTunnel(MultiP2PTunnelPart<?> part) {
        return getTunnel(part.getTunnelID(), part.getFrequency(), part::createTunnel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeNode(IGridNode node) {
        // needs a raw reference because I can't figure out how else to get the generics to work
        // okay here because the part should always reference the right type of tunnel
        if (node.getOwner() instanceof MultiP2PTunnelPart part) {
            // this will leave an empty MultiTunnel in memory, but the player might make parts with the freq again, otherwise it'll get wiped on world restart or something
            getTunnel(part).removeTunnel(part);
            this.updateTunnel(tunnel.getFrequency(), !tunnel.isOutput(), false);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addNode(IGridNode node, @Nullable CompoundTag savedData) {
        // raw for same reasons in removeNode
        if (node.getOwner() instanceof MultiP2PTunnelPart part) {
            getTunnel(part).removeTunnel(part);
            this.updateTunnel(tunnel.getFrequency(), !tunnel.isOutput(), false);
        }
    }

    private void updateTunnel(short freq, boolean updateOutputs, boolean configChange) {
        if (updateOutputs)
            {
                for (MultiP2PTunnelPart<?> p : this.outputs.get(freq))
                    {
                        if (configChange)
                            {
                                p.onTunnelConfigChange();
                            }
                        p.onTunnelNetworkChange();
                    }
            }
        if (!updateOutputs)
            {
                final Collection<MultiP2PTunnelPart<?>> in = this.inputs.get(freq);
                if (in != null)
                    {
                        if (configChange)
                            {
                                in.forEach(part -> part.onTunnelConfigChange());
                            }
                        in.forEach(part -> part.onTunnelNetworkChange());
                    }
            }
    }

    public void updateFreq(MultiP2PTunnelPart<?> t, short newFrequency) {
        if (this.outputs.containsValue(t))
            {
                this.outputs.remove(t.getFrequency(), t);
            }

        if (this.inputs.containsValue(t))
            {
                this.inputs.remove(t.getFrequency(), t);
            }

        var oldFrequency = t.getFrequency();
        t.setFrequency(newFrequency);

        if (t.isOutput())
            {
                this.outputs.put(t.getFrequency(), t);
            } else
            {
                this.inputs.put(t.getFrequency(), t);
            }

        if (oldFrequency != newFrequency)
            {
                this.updateTunnel(oldFrequency, true, true);
                this.updateTunnel(oldFrequency, false, true);
            }
        this.updateTunnel(newFrequency, true, true);
        this.updateTunnel(newFrequency, false, true);
    }

    public short newFrequency() {
        short newFrequency;
        int cycles = 0;

        do
            {
                newFrequency = (short) this.frequencyGenerator.nextInt(1 << 16);
                cycles++;
            } while (newFrequency == 0 || this.inputs.containsKey(newFrequency));

        if (cycles > 25)
            {
                MAE2.LOGGER.debug("Generating a new MultiP2P frequency '%1$d' took %2$d cycles",
                                  newFrequency,
                                  cycles);
            }

        return newFrequency;
    }

    public <T extends MultiP2PTunnelPart<T>> Stream<T> getOutputs(short freq, Class<T> c) {
        return this.outputs.get(freq).stream().filter(c::isInstance).map(c::cast);
    }

    public <T extends MultiP2PTunnelPart<T>> Stream<T> getInputs(short freq, Class<T> c) {
        return this.inputs.get(freq).stream().filter(c::isInstance).map(c::cast);
    }

    /**
     * Registers your {@link MultiP2PTunnel} to the registry
     *
     * Currently does nothing but return the tunnel id, you **need** to save this id and return it in {@link MultiP2PTunnel}
     *
     * @return the tunnel's id
     */
    public static short register() {
        return (short) ID_COUNTER.getAndIncrement();
    }
}

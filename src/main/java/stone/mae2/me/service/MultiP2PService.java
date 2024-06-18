package stone.mae2.me.service;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.events.GridBootingStatusChange;
import appeng.api.networking.events.GridPowerStatusChange;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import stone.mae2.MAE2;
import stone.mae2.parts.p2p.MEMultiP2PTunnelPart;
import stone.mae2.parts.p2p.MultiP2PTunnelPart;

import java.util.Collection;
import java.util.Random;
import java.util.stream.Stream;

/**
 * 
 */
public class MultiP2PService implements IGridService, IGridServiceProvider {
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
    private final Multimap<Short, MultiP2PTunnelPart<?>> inputs = LinkedHashMultimap.create();
    private final Multimap<Short, MultiP2PTunnelPart<?>> outputs = LinkedHashMultimap.create();
    private final Random frequencyGenerator;

    public MultiP2PService(IGrid g) {
        this.myGrid = g;
        // add one to hopefully prevent both P2P services from making the same frequencies
        this.frequencyGenerator = new Random(g.hashCode() + 1);
    }

    public void wakeInputTunnels() {
        var tm = this.myGrid.getTickManager();
        for (var tunnel : this.inputs.values())
        {
            if (tunnel instanceof MEMultiP2PTunnelPart)
            {
                tm.wakeDevice(tunnel.getGridNode());
            }
        }
    }

    @Override
    public void removeNode(IGridNode node) {
        if (node.getOwner() instanceof MultiP2PTunnelPart<?> tunnel)
        {
            if (tunnel instanceof MEMultiP2PTunnelPart && !node.hasFlag(GridFlags.REQUIRE_CHANNEL))
            {
                return;
            }

            if (tunnel.isOutput())
            {
                this.outputs.remove(tunnel.getFrequency(), tunnel);
            } else
            {
                this.inputs.remove(tunnel.getFrequency(), tunnel);
            }

            this.updateTunnel(tunnel.getFrequency(), !tunnel.isOutput(), false);
        }
    }

    @Override
    public void addNode(IGridNode node, @Nullable CompoundTag savedData) {
        if (node.getOwner() instanceof MultiP2PTunnelPart<?> tunnel)
        {
            if (tunnel instanceof MEMultiP2PTunnelPart && !node.hasFlag(GridFlags.REQUIRE_CHANNEL))
            {
                return;
            }

            // AELog.info( "add-" + (t.output ? "output: " : "input: ") + t.freq );

            if (tunnel.isOutput())
            {
                this.outputs.put(tunnel.getFrequency(), tunnel);
            } else
            {
                this.inputs.put(tunnel.getFrequency(), tunnel);
            }

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
}

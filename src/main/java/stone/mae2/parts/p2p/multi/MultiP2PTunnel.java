package stone.mae2.parts.p2p.multi;

import java.util.HashSet;
import java.util.Set;

import stone.mae2.me.service.MultiP2PService;

public abstract class MultiP2PTunnel<T extends MultiP2PTunnelPart<T>> {
    protected final Set<MultiP2PTunnelPart<T>> inputs = new HashSet<>();
    protected final Set<MultiP2PTunnelPart<T>> outputs = new HashSet<>();

    /**
     * Adds a {@link MultiP2PTunnelPart} to this {@link MultiP2PTunnel}, input/ouput is automatically decided
     *
     * @return if the part was already in the tunnel
     */
    public boolean addTunnel(MultiP2PTunnelPart<T> tunnel) {
        if (tunnel.isOutput()) {
            return outputs.add(tunnel);
        } else {
            return inputs.add(tunnel);
        }
    }

    /**
     * Removes a {@link MultiP2PTunnelPart} to this {@link MultiP2PTunnel}, input/output is automatically decided
     *
     * @return if the part was already in the tunnel
     */
    public boolean removeTunnel(MultiP2PTunnelPart<T> tunnel) {
        if (tunnel.isOutput()) {
            return outputs.remove(tunnel);
        } else {
            return inputs.remove(tunnel);
        }
    }

    public static void register() {
        MultiP2PService.register();
    }
}

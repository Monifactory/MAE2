package stone.mae2.api.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import appeng.api.parts.IPartItem;
import appeng.parts.p2p.P2PTunnelPart;
import stone.mae2.parts.p2p.multi.MultiP2PTunnelPart;

public final class MultiP2PTunnelAttunement {
    private static final Map<IPartItem<? extends P2PTunnelPart<?>>, IPartItem<? extends MultiP2PTunnelPart<?>>> single2MultiAttunements = new HashMap<>();

    public synchronized static IPartItem<? extends MultiP2PTunnelPart<?>> getMultiPartBySinglePart(IPartItem<? extends P2PTunnelPart<?>> single) {
        return single2MultiAttunements.get(single);
    }

    public synchronized static void registerAttunementItem(IPartItem<? extends P2PTunnelPart<?>> single, IPartItem<? extends MultiP2PTunnelPart<?>> multi) {
        Objects.requireNonNull(single, "Single Tunnel version can't be null!");
        Objects.requireNonNull(multi, "Multi Tunnel version can't  be null!");
        single2MultiAttunements.put(single, multi);
    }

    public static boolean hasRegisteredMulti(IPartItem<P2PTunnelPart<?>> single) {
        return single2MultiAttunements.get(single) == null;
    }
}

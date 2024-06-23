package stone.mae2.api.features;

import appeng.api.parts.IPartItem;
import appeng.core.definitions.AEParts;
import appeng.parts.p2p.P2PTunnelPart;

import stone.mae2.core.MAE2Items;
import stone.mae2.parts.p2p.multi.MultiP2PTunnelPart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public static Map<IPartItem<? extends P2PTunnelPart<?>>, IPartItem<? extends MultiP2PTunnelPart<?>>> getRegistry() {
        return single2MultiAttunements;
    }

    public static void registerStockAttunements() {
        // Stock Multi P2P Attunements
        MultiP2PTunnelAttunement.registerAttunementItem(MAE2Items.PATTERN_P2P_TUNNEL.get(),
            MAE2Items.PATTERN_MULTI_P2P_TUNNEL.get());
        MultiP2PTunnelAttunement.registerAttunementItem(AEParts.REDSTONE_P2P_TUNNEL.asItem(),
            MAE2Items.REDSTONE_MULTI_P2P_TUNNEL.get());

        MultiP2PTunnelAttunement.registerAttunementItem(AEParts.FE_P2P_TUNNEL.asItem(),
            MAE2Items.FE_MULTI_P2P_TUNNEL.get());
        MultiP2PTunnelAttunement.registerAttunementItem(AEParts.FLUID_P2P_TUNNEL.asItem(),
            MAE2Items.FLUID_MULTI_P2P_TUNNEL.get());
        MultiP2PTunnelAttunement.registerAttunementItem(AEParts.ITEM_P2P_TUNNEL.asItem(),
            MAE2Items.ITEM_MULTI_P2P_TUNNEL.get());
    }
}

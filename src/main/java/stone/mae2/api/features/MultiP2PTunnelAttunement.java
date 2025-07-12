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
package stone.mae2.api.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import appeng.api.parts.IPartItem;
import appeng.parts.p2p.P2PTunnelPart;
import stone.mae2.parts.p2p.multi.MultiP2PTunnel;

public final class MultiP2PTunnelAttunement {
	
    //private static final Map<IPartItem<? extends P2PTunnelPart<?>>, IPartItem<? extends MultiP2PTunnel.Part<?, ?, ?>>> single2MultiAttunements = new HashMap<>();
	private static final Map<IPartItem<? extends P2PTunnelPart<?>>, IPartItem<? extends MultiP2PTunnel.Part>> REGISTRY = new HashMap<>();
    public synchronized static IPartItem<? extends MultiP2PTunnel.Part> getMultiPartBySinglePart(IPartItem<? extends P2PTunnelPart<?>> single) {
        return REGISTRY.get(single);
    }

    public synchronized static void registerAttunementItem(IPartItem<? extends P2PTunnelPart<?>> single, IPartItem<? extends MultiP2PTunnel.Part> multi) {
        Objects.requireNonNull(single, "Single Tunnel version can't be null!");
        Objects.requireNonNull(multi, "Multi Tunnel version can't  be null!");
        REGISTRY.put(single, multi);
    }

    public static boolean hasRegisteredMulti(IPartItem<P2PTunnelPart<?>> single) {
        return REGISTRY.get(single) == null;
    }

    public static Map<IPartItem<? extends P2PTunnelPart<?>>, IPartItem<? extends MultiP2PTunnel.Part>> getRegistry() {
        return REGISTRY;
    }

    public static void registerStockAttunements() {
        // Stock Multi P2P Attunements
    	/*
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
          */  
    }
}

/*
 * Copyright (C) 2024 AE2 Enthusiast
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
package stone.mae2.integration;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.bootstrap.MAE2Items;
import stone.mae2.parts.p2p.EUP2PTunnelPart;
import stone.mae2.parts.p2p.multi.EUMultiP2PTunnel;

public abstract class GregTechIntegration {
  public static RegistryObject<PartItem<EUP2PTunnelPart>> EU_P2P_TUNNEL;

  public static RegistryObject<PartItem<EUMultiP2PTunnel.Part>> EU_MULTI_P2P_TUNNEL;

  public static void init(IEventBus bus) {
    if (MAE2.CONFIG.parts().isEUP2PEnabled()) {
      EU_P2P_TUNNEL = Util.make(() -> {
        PartModels
          .registerModels(PartModelsHelper.createModels(EUP2PTunnelPart.class));
        return MAE2Items.ITEMS
          .register("eu_p2p_tunnel", () -> new PartItem<>(new Item.Properties(),
            EUP2PTunnelPart.class, EUP2PTunnelPart::new));
      });

      EU_MULTI_P2P_TUNNEL = Util.make(() -> {
        PartModels
          .registerModels(
            PartModelsHelper.createModels(EUMultiP2PTunnel.Part.class));
        return MAE2Items.ITEMS
          .register("eu_multi_p2p_tunnel",
            () -> new PartItem<>(new Item.Properties(),
              EUMultiP2PTunnel.Part.class, EUMultiP2PTunnel.Part::new));
      });

      bus.addListener((FMLCommonSetupEvent event) -> {
        P2PTunnelAttunement.registerAttunementTag(EU_P2P_TUNNEL.get());
        MultiP2PTunnelAttunement
          .registerAttunementItem(EU_P2P_TUNNEL.get(),
            EU_MULTI_P2P_TUNNEL.get());
      });
    }
  }
}

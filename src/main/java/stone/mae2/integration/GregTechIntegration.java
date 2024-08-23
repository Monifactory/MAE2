package stone.mae2.integration;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.core.MAE2Items;
import stone.mae2.parts.p2p.EUP2PTunnelPart;
import stone.mae2.parts.p2p.multi.EUMultiP2PPart;

public abstract class GregTechIntegration {
	public static RegistryObject<PartItem<EUP2PTunnelPart>> EU_P2P_TUNNEL;
	
	public static RegistryObject<PartItem<EUMultiP2PPart>> EU_MULTI_P2P_TUNNEL;
	
	public static void init(IEventBus bus) {
		EU_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(EUP2PTunnelPart.class));
            return MAE2Items.ITEMS.register("eu_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            EUP2PTunnelPart.class, EUP2PTunnelPart::new));
        });
		
		EU_MULTI_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(EUMultiP2PPart.class));
            return MAE2Items.ITEMS.register("eu_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(), EUMultiP2PPart.class, EUMultiP2PPart::new));
        });
		
		bus.addListener((FMLCommonSetupEvent event) -> {
		    P2PTunnelAttunement.registerAttunementTag(EU_P2P_TUNNEL.get());
            P2PTunnelAttunement
                .registerAttunementApi(EU_P2P_TUNNEL.get(), GTCapability.CAPABILITY_ELECTRIC_ITEM,
                    Component.literal("Item with EU storage"));
			MultiP2PTunnelAttunement.registerAttunementItem(EU_P2P_TUNNEL.get(), EU_MULTI_P2P_TUNNEL.get());
		});
	}
}

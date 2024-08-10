package stone.mae2.integration;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import stone.mae2.core.MAE2Items;
import stone.mae2.parts.p2p.EUP2PTunnelPart;
import stone.mae2.parts.p2p.PatternP2PTunnelPart;

public abstract class GregTechIntegration {
	public static RegistryObject<PartItem<EUP2PTunnelPart>> EU_P2P_TUNNEL;
	
	public static void init(IEventBus bus) {
		EU_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(EUP2PTunnelPart.class));
            return MAE2Items.ITEMS.register("eu_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            EUP2PTunnelPart.class, EUP2PTunnelPart::new));
        });
	}
}

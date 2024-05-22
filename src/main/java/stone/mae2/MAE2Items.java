package stone.mae2;

import stone.mae2.parts.PatternP2PTunnelPart;

import net.minecraft.Util;
import net.minecraft.world.item.Item;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;

public class MAE2Items {

    public final DeferredRegister<Item> ITEMS = DeferredRegister
        .create(ForgeRegistries.ITEMS, MAE2.MODID);

    public void init(IEventBus bus) {
        ITEMS.register(bus);
    }

    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AECreativeTabIds.MAIN)
        {
            event.accept(INTERFACE_P2P_TUNNEL);
        }
    }

    public final PartItem<PatternP2PTunnelPart> INTERFACE_P2P_TUNNEL = Util
        .make(() ->
        {
        PartModels.registerModels(PartModelsHelper.createModels(PatternP2PTunnelPart.class));
        return ITEMS.register("interface_p2p_tunnel",
            () -> new PartItem<>(new Item.Properties(),
                PatternP2PTunnelPart.class, PatternP2PTunnelPart::new))
            .get();
    });
}

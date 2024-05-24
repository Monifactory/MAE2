package stone.mae2;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.parts.PatternP2PTunnelPart;

public class MAE2Items {

    private final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
        .create(Registries.CREATIVE_MODE_TAB, MAE2.MODID);
    public final DeferredRegister<Item> ITEMS = DeferredRegister
        .create(ForgeRegistries.ITEMS, MAE2.MODID);



    public void init(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    public final RegistryObject<PartItem<PatternP2PTunnelPart>> PATTERN_P2P_TUNNEL = Util
        .make(() ->
        {
        PartModels.registerModels(PartModelsHelper.createModels(PatternP2PTunnelPart.class));
            return ITEMS.register("pattern_p2p_tunnel",
            () -> new PartItem<>(new Item.Properties(),
                    PatternP2PTunnelPart.class, PatternP2PTunnelPart::new));
    });

    public final RegistryObject<CreativeModeTab> CREATIVE_TAB = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(
                Component.translatable("gui." + MAE2.MODID + ".creative_tab"))
            .icon(() -> new ItemStack(PATTERN_P2P_TUNNEL.get()))
            .displayItems((params, output) ->
            {
                for (var entry : ITEMS.getEntries())
                {
                    output.accept(entry.get());
                }
            }).build());
}

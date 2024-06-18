package stone.mae2.core;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.block.crafting.CraftingBlockItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.parts.p2p.PatternP2PTunnelPart;
import stone.mae2.parts.p2p.multi.PatternMultiP2PPart;

public abstract class MAE2Items {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister
            .create(ForgeRegistries.ITEMS, MAE2.MODID);

    public static RegistryObject<PartItem<PatternP2PTunnelPart>> PATTERN_P2P_TUNNEL;
    public static RegistryObject<PartItem<PatternMultiP2PPart>> MULTI_PATTERN_P2P_TUNNEL;

    public static RegistryObject<CraftingBlockItem> ACCELERATOR_4x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_16x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_64x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_256x;

    public static void init(IEventBus bus) {
        register();
        ITEMS.register(bus);

        bus.addListener((FMLCommonSetupEvent event) -> {
            P2PTunnelAttunement
                    .registerAttunementTag(PATTERN_P2P_TUNNEL.get());
        });
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        PATTERN_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(PatternP2PTunnelPart.class));
            return ITEMS.register("pattern_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            PatternP2PTunnelPart.class, PatternP2PTunnelPart::new));
        });
        MULTI_PATTERN_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(PatternMultiP2PPart.class));
            return ITEMS.register("pattern_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(), PatternMultiP2PPart.class, PatternMultiP2PPart::new));
        });

    }

}

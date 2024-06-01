package stone.mae2.core;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.block.crafting.CraftingBlockItem;
import appeng.items.materials.StorageComponentItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import appeng.items.storage.BasicStorageCell;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.parts.PatternP2PTunnelPart;

public abstract class MAE2Items {


    public static final DeferredRegister<Item> ITEMS = DeferredRegister
        .create(ForgeRegistries.ITEMS, MAE2.MODID);

    public static RegistryObject<PartItem<PatternP2PTunnelPart>> PATTERN_P2P_TUNNEL;


    public static RegistryObject<CraftingBlockItem> ACCELERATOR_4x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_16x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_64x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_256x;

    public static RegistryObject<StorageComponentItem> COMPONENT_MAX;
    public static RegistryObject<BasicStorageCell> CELL_MAX;

    public static RegistryObject<CraftingBlockItem> STORAGE_MAX;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_MAX;


    public static void init(IEventBus bus) {
        register();
        ITEMS.register(bus);


        bus.addListener((FMLCommonSetupEvent event) ->
        {

            if (MAE2Config.isInterfaceP2PEnabled)
            {
                P2PTunnelAttunement
                    .registerAttunementTag(PATTERN_P2P_TUNNEL.get());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        // always registers pattern p2p for the creative tab's icon
        // TODO figure something out for that
        PATTERN_P2P_TUNNEL = Util.make(() ->
        {
            PartModels.registerModels(
                PartModelsHelper.createModels(PatternP2PTunnelPart.class));
            return ITEMS.register("pattern_p2p_tunnel",
                () -> new PartItem<>(new Item.Properties(),
                    PatternP2PTunnelPart.class, PatternP2PTunnelPart::new));
        });

        if (MAE2Config.isMAXTierEnabled)
        {
            COMPONENT_MAX = ITEMS.register("cell_component_max",
                () -> new StorageComponentItem(new Item.Properties(),
                    Integer.MAX_VALUE / 8096));
            // MAX_CELL = ITEMS.register("universal_storage_cell_max", () -> new
            // BasicStorageCell(null, null, null, 0, 0, 0, 0, null));
        }
    }


}

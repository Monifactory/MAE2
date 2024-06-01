package stone.mae2.core;

import appeng.block.crafting.CraftingBlockItem;
import appeng.block.crafting.CraftingUnitBlock;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.block.crafting.DynamicCraftingBlockItem;
import stone.mae2.block.crafting.DynamicCraftingUnitType;

public abstract class MAE2Blocks {
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
        MAE2.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
        .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MAE2.MODID);

    public static RegistryObject<CraftingUnitBlock> ACCELERATOR_4x;
    public static RegistryObject<CraftingUnitBlock> ACCELERATOR_16x;
    public static RegistryObject<CraftingUnitBlock> ACCELERATOR_64x;
    public static RegistryObject<CraftingUnitBlock> ACCELERATOR_256x;

    public static RegistryObject<CraftingUnitBlock> STORAGE_MAX;
    public static RegistryObject<CraftingUnitBlock> ACCELERATOR_MAX;

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        register(bus);

        MAE2BlockEntities.init(bus);
    }

    @SuppressWarnings("unchecked")
    public static void register(IEventBus bus) {
        if (MAE2Config.areDenseCoprocessersEnabled)
        {
            ACCELERATOR_4x = BLOCKS.register("4x_crafting_accelerator",
                () -> new CraftingUnitBlock(DynamicCraftingUnitType.ACCELERATOR_4x));

            MAE2Items.ACCELERATOR_4x = MAE2Items.ITEMS.register("4x_crafting_accelerator",
                () -> new DynamicCraftingBlockItem(ACCELERATOR_4x.get(), new Item.Properties(),
                    () -> AEBlocks.CRAFTING_ACCELERATOR.asItem(),
                    () -> AEItems.CELL_COMPONENT_4K.asItem()));

            ACCELERATOR_16x = BLOCKS.register("16x_crafting_accelerator",
                () -> new CraftingUnitBlock(DynamicCraftingUnitType.ACCELERATOR_16x));

            MAE2Items.ACCELERATOR_16x = MAE2Items.ITEMS.register("16x_crafting_accelerator",
                () -> new DynamicCraftingBlockItem(ACCELERATOR_16x.get(), new Item.Properties(),
                    () -> AEBlocks.CRAFTING_ACCELERATOR.asItem(),
                    () -> AEItems.CELL_COMPONENT_16K.asItem()));

            ACCELERATOR_64x = BLOCKS.register("64x_crafting_accelerator",
                () -> new CraftingUnitBlock(DynamicCraftingUnitType.ACCELERATOR_64x));

            MAE2Items.ACCELERATOR_64x = MAE2Items.ITEMS.register("64x_crafting_accelerator",
                () -> new DynamicCraftingBlockItem(ACCELERATOR_64x.get(), new Item.Properties(),
                    () -> AEBlocks.CRAFTING_ACCELERATOR.asItem(),
                    () -> AEItems.CELL_COMPONENT_64K.asItem()));

            ACCELERATOR_256x = BLOCKS.register("256x_crafting_accelerator",
                () -> new CraftingUnitBlock(DynamicCraftingUnitType.ACCELERATOR_256x));

            MAE2Items.ACCELERATOR_256x = MAE2Items.ITEMS.register("256x_crafting_accelerator",
                () -> new DynamicCraftingBlockItem(ACCELERATOR_256x.get(), new Item.Properties(),
                    () -> AEBlocks.CRAFTING_ACCELERATOR.asItem(),
                    () -> AEItems.CELL_COMPONENT_256K.asItem()));
        }

        if (MAE2Config.isMAXTierEnabled)
        {
            STORAGE_MAX = BLOCKS.register("max_crafting_storage",
                () -> new CraftingUnitBlock(DynamicCraftingUnitType.STORAGE_MAX));
            MAE2Items.STORAGE_MAX = MAE2Items.ITEMS.register("max_crafting_storage",
                () -> new CraftingBlockItem(STORAGE_MAX.get(), new Item.Properties(),
                    () -> MAE2Items.COMPONENT_MAX.get()));

            ACCELERATOR_MAX = BLOCKS.register("max_crafting_accelerator",
                () -> new CraftingUnitBlock(DynamicCraftingUnitType.ACCELERATOR_MAX));
            MAE2Items.ACCELERATOR_MAX = MAE2Items.ITEMS.register("max_crafting_accelerator",
                () -> new DynamicCraftingBlockItem(ACCELERATOR_MAX.get(), new Item.Properties(),
                    () -> AEBlocks.CRAFTING_ACCELERATOR.asItem(),
                    () -> MAE2Items.COMPONENT_MAX.get()));

        }
    }
}

package stone.mae2.core;

import appeng.block.crafting.CraftingBlockItem;
import appeng.block.crafting.CraftingUnitBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.block.crafting.DynamicCraftingUnitType;

public abstract class MAE2Blocks {
    static final DeferredRegister<Block> BLOCKS = DeferredRegister
        .create(ForgeRegistries.BLOCKS, MAE2.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
        .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MAE2.MODID);


    public static RegistryObject<CraftingUnitBlock>[] DENSE_ACCELERATORS;
    public static RegistryObject<CraftingUnitBlock> MAX_STORAGE;
    public static RegistryObject<CraftingUnitBlock> MAX_ACCELERATOR;

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        register(bus);

        MAE2BlockEntities.init(bus);
    }

    @SuppressWarnings("unchecked")
    public static void register(IEventBus bus) {
        if (MAE2Config.areDenseCoprocessersEnabled)
        {
            MAE2Blocks.DENSE_ACCELERATORS = new RegistryObject[4];
            MAE2Items.DENSE_ACCELERATORS = new RegistryObject[4];
            for (int i = 0; i < DENSE_ACCELERATORS.length; i++)
            {
                final int tier = (int) Math.pow(4, i + 1);
                String name = tier + "x_coprocessor";
                RegistryObject<CraftingUnitBlock> accelerator = BLOCKS
                    .register(name, () -> new CraftingUnitBlock(
                        new DynamicCraftingUnitType(0, tier)));
                DENSE_ACCELERATORS[i] = accelerator;

                MAE2Items.DENSE_ACCELERATORS[i] = MAE2Items.ITEMS.register(name,
                    () -> new CraftingBlockItem(accelerator.get(),
                        new Item.Properties(), () -> Items.AIR));
            }
        }

        if (MAE2Config.isMAXTierEnabled)
        {
            MAX_STORAGE = BLOCKS.register("max_crafting_storage",
                () -> new CraftingUnitBlock(
                    new DynamicCraftingUnitType(Long.MAX_VALUE, 0)));
            MAE2Items.MAX_STORAGE = MAE2Items.ITEMS.register(
                "max_crafting_storage",
                () -> new CraftingBlockItem(MAX_STORAGE.get(),
                    new Item.Properties(), () -> Items.AIR));

            MAX_ACCELERATOR = BLOCKS.register("max_crafting_accelerator",
                () -> new CraftingUnitBlock(
                    new DynamicCraftingUnitType(0, Integer.MAX_VALUE)));
            MAE2Items.MAX_ACCELERATOR = MAE2Items.ITEMS.register(
                "max_crafting_accelerator",
                () -> new CraftingBlockItem(MAX_ACCELERATOR.get(),
                    new Item.Properties(), () -> Items.AIR));

        }
    }
}

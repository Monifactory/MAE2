package stone.mae2.registration;

import appeng.block.AEBaseBlockItem;
import appeng.block.crafting.CraftingUnitBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.blocks.crafting.DynamicCraftingUnitType;

public abstract class MAE2Blocks {
    static final DeferredRegister<Block> BLOCKS = DeferredRegister
            .create(ForgeRegistries.BLOCKS, MAE2.MODID);

    static RegistryObject<Block>[] EXTRA_CRAFTING_COPROCESSORS;
    static RegistryObject<Block>[] EXtRA_CRAFTING_STORAGE;

    public static void init(IEventBus bus) {
        registerBlocks();
        registerItems();
        BLOCKS.register(bus);
    }

    @SuppressWarnings("unchecked")
    public static void registerBlocks() {
        if (MAE2Config.isExtraCoproccesorsEnabled) {
            EXTRA_CRAFTING_COPROCESSORS = new RegistryObject[4];
            for (int i = 0; i < EXTRA_CRAFTING_COPROCESSORS.length; i++) {
                final int tier = i + 1;
                EXTRA_CRAFTING_COPROCESSORS[i] = BLOCKS.register("coproccesor" + i,
                        () -> new CraftingUnitBlock(new DynamicCraftingUnitType(null, 0, (int) Math.pow(4, tier))));

            }
        }
    }

    public static void registerItems() {
        if (MAE2Config.isExtraCoproccesorsEnabled) {
            for (int i = 0; i < EXTRA_CRAFTING_COPROCESSORS.length; i++) {
                final int tier = i;
                MAE2Items.EXTRA_CRAFTING_COPROCESSORS[i] = MAE2Items.ITEMS.register("coproccesor" + i,
                                                                                    () -> new AEBaseBlockItem(EXTRA_CRAFTING_COPROCESSORS[tier].get(), new Item.Properties()));
            }
        }
    }
}

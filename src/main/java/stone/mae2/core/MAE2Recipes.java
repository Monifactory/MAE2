package stone.mae2.core;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import stone.mae2.MAE2;

import java.util.function.Consumer;

public abstract class MAE2Recipes {

    public static void init(IEventBus bus) {
        bus.addListener((GatherDataEvent event) ->
        {
            DataGenerator gen = event.getGenerator();
            DataGenerator.PackGenerator pack = gen.getVanillaPack(true);

            pack.addProvider(MAE2RecipeProvider::new);
        });
    }

    public static class MAE2RecipeProvider extends RecipeProvider {

        public MAE2RecipeProvider(PackOutput output) {
            super(output);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
            // TODO Auto-generated method stub

            if (MAE2Config.areDenseCoprocessersEnabled)
            {
                buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_4x.get(),
                    AEItems.CELL_COMPONENT_4K, "network/crafting/4x_crafting_accelerator");
                buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_16x.get(),
                    AEItems.CELL_COMPONENT_16K, "network/crafting/16x_crafting_accelerator");
                buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_64x.get(),
                    AEItems.CELL_COMPONENT_64K, "network/crafting/64x_crafting_accelerator");
                buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_256x.get(),
                    AEItems.CELL_COMPONENT_256K, "network/crafting/256x_crafting_accelerator");
            }

            if (MAE2Config.isMAXTierEnabled)
            {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MAE2Items.STORAGE_MAX.get())
                    .requires(MAE2Items.COMPONENT_MAX.get())
                    .requires(AEBlocks.CRAFTING_UNIT.asItem())
                    .unlockedBy("has_crafting_unit", has(AEBlocks.CRAFTING_UNIT))
                    .save(consumer, MAE2.toKey("network/crafting/max_crafting_storage"));
                buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_MAX.get(),
                    MAE2Items.COMPONENT_MAX.get(), "network/crafting/max_crafting_accelerator");
            }
        }

        public static void buildAcceleratorRecipe(Consumer<FinishedRecipe> consumer,
            ItemLike output, ItemLike component, String id) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, output).requires(component)
                .requires(AEBlocks.CRAFTING_ACCELERATOR.asItem())
                .unlockedBy("has_acceleration_unit", has(AEBlocks.CRAFTING_ACCELERATOR))
                .save(consumer, MAE2.toKey(id));
        }

    }

}

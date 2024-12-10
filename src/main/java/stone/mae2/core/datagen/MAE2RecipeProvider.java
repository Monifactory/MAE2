package stone.mae2.core.datagen;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import stone.mae2.MAE2;
import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.bootstrap.MAE2Items;

import java.util.function.Consumer;

public class MAE2RecipeProvider extends RecipeProvider {

    public MAE2RecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_4x.get(), AEItems.CELL_COMPONENT_4K,
            "network/crafting/4x_crafting_accelerator");
        buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_16x.get(),
            AEItems.CELL_COMPONENT_16K, "network/crafting/16x_crafting_accelerator");
        buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_64x.get(),
            AEItems.CELL_COMPONENT_64K, "network/crafting/64x_crafting_accelerator");
        buildAcceleratorRecipe(consumer, MAE2Items.ACCELERATOR_256x.get(),
            AEItems.CELL_COMPONENT_256K, "network/crafting/256x_crafting_accelerator");

        MultiP2PTunnelAttunement.registerStockAttunements();
        for (var tunnelPair : MultiP2PTunnelAttunement.getRegistry().entrySet())
        {
            String simpleName = tunnelPair.getValue().getPartClass().getSimpleName();
            buildMultiP2PRecipe(consumer, tunnelPair.getValue(), tunnelPair.getKey(),
                    MAE2.toKey("network/parts/multi_p2p_tunnel_"
                            + simpleName.substring(0, simpleName.length() - "MultiP2PPart".length())
                                    .toLowerCase()));
        }

        // stopgap for the fact that ME Multi P2Ps don't exist (so there's no direct
        // crafting recipe to Multi P2Ps, you'd need to attune a ME P2P to something
        // else before crafting the Multi P2P)
        buildMultiP2PRecipe(consumer, MAE2Items.ITEM_MULTI_P2P_TUNNEL.get(), AEParts.ME_P2P_TUNNEL, MAE2.toKey("network/parts/multi_p2p_tunnel_workaround"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MAE2Items.FAULTY_MEMORY_CARD.get())
            .pattern("lcc")
            .pattern("grg")
            .define('l', AEItems.LOGIC_PROCESSOR)
            .define('c', Items.COPPER_INGOT)
            .define('g', Items.GOLD_INGOT)
            .define('r', Items.REDSTONE)
            .unlockedBy("has_memory_card", has(AEItems.MEMORY_CARD))
            .save(consumer);
            }

    public static void buildAcceleratorRecipe(Consumer<FinishedRecipe> consumer, ItemLike output,
        ItemLike component, String id) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, output).requires(component)
            .requires(AEBlocks.CRAFTING_ACCELERATOR.asItem())
            .unlockedBy("has_acceleration_unit", has(AEBlocks.CRAFTING_ACCELERATOR))
            .save(consumer, MAE2.toKey(id));
    }

    public static void buildMultiP2PRecipe(Consumer<FinishedRecipe> consumer, ItemLike multi,
        ItemLike single, ResourceLocation id) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, multi).requires(single)
            .requires(AEItems.ENGINEERING_PROCESSOR)
            .unlockedBy("has_me_p2p", has(AEParts.ME_P2P_TUNNEL))
            .save(consumer, id);
    }

}

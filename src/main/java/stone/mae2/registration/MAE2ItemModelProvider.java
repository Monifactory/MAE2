package stone.mae2.registration;

import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import stone.mae2.MAE2;

public class MAE2ItemModelProvider extends ItemModelProvider
    implements DataProvider {

    public MAE2ItemModelProvider(PackOutput output,
        ExistingFileHelper existingFileHelper) {
        super(output, MAE2.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ModelFile itemGenerated = new ModelFile.UncheckedModelFile(
            "item/generated");

        MAE2.LOGGER.debug("Registering Models");

        if (MAE2Config.areExtraTiersEnabled)
        {
            for (int i = 0; i < MAE2Items.STORAGE_COMPONENTS.length; i++)
            {
                for (int j = 0; j < MAE2Config.extraStoreageSubTierCount; j++)
                {

                    ResourceLocation item = MAE2Items.STORAGE_COMPONENTS[i][j]
                        .getId();
                    MAE2.LOGGER.debug("Registering model for item {}",
                        item.getNamespace());
                    /*
                     * withExistingParent(item.getPath(),
                     * mcLoc("item/generated")) // ("item/" +
                     * item.getPath()).parent(new //
                     * ModelFile.UncheckedModelFile( // "item/generated"))
                     * .texture("layer0", modLoc("item/storage_component" + j))
                     * .texture("layer1", modLoc( "item/storage_component" + j +
                     * "_overlay"));
                     */

                    singleTexture(item.getPath(), mcLoc("item/generated"),
                        "layer0", modLoc("item/storage_component" + j))
                        .texture("layer1",
                            "item/storage_component" + j + "_overlay");

                }
            }
        }
    }
}

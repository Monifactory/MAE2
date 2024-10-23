package stone.mae2.client.render.model;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

import appeng.client.render.BasicUnbakedModel;

import stone.mae2.MAE2;

/**
 * Model wrapper for the memory card item model, which combines a base card layer with a "visual hash" of the part/tile.
 *
 * Copied from AE2
 */
public class FaultyCardModel implements BasicUnbakedModel {

    public static final ResourceLocation MODEL_BASE = MAE2.toKey("item/faulty_card_base");
    private static final Material TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS,
            MAE2.toKey("item/faulty_card_hash"));

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singleton(MODEL_BASE);
    }

    @Nullable
    @Override
    public BakedModel bake(ModelBaker loader, Function<Material, TextureAtlasSprite> textureGetter,
            ModelState rotationContainer, ResourceLocation modelId) {
        TextureAtlasSprite texture = textureGetter.apply(TEXTURE);

        BakedModel baseModel = loader.bake(MODEL_BASE, rotationContainer);

        return new FaultyCardBakedModel(baseModel, texture);
    }

}

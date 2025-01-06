/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see https://www.gnu.org/licenses/.
 */
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

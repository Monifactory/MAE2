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
package stone.mae2.client.render.crafting;

import appeng.client.render.crafting.AbstractCraftingUnitModelProvider;
import appeng.client.render.crafting.LightBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import stone.mae2.MAE2;
import stone.mae2.block.crafting.DynamicCraftingUnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class DynamicCraftingCubeModelProvider
    extends AbstractCraftingUnitModelProvider<DynamicCraftingUnitType> {

    public static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final List<Material> MATERIALS = new ArrayList<>();

    protected final static Material RING_CORNER = texture(MAE2.MODID, "ring_corner");
    protected final static Material RING_SIDE_HOR = texture(MAE2.MODID, "ring_side_hor");
    protected final static Material RING_SIDE_VER = texture(MAE2.MODID, "ring_side_ver");
    protected final static Material LIGHT_BASE = texture(MAE2.MODID, "light_base");

    protected final static Material ACCELERATOR_4x_LIGHT = texture(MAE2.MODID,
        "4x_accelerator_light");
    protected final static Material ACCELERATOR_16x_LIGHT = texture(MAE2.MODID,
        "16x_accelerator_light");
    protected final static Material ACCELERATOR_64x_LIGHT = texture(MAE2.MODID,
        "64x_accelerator_light");
    protected final static Material ACCELERATOR_256x_LIGHT = texture(MAE2.MODID,
        "256x_accelerator_light");

    public DynamicCraftingCubeModelProvider(DynamicCraftingUnitType type) {
        super(type);
    }

    @Override
    public List<Material> getMaterials() {
        return Collections.unmodifiableList(MATERIALS);
    }

    @Override
    public BakedModel getBakedModel(Function<Material, TextureAtlasSprite> spriteGetter) {
        TextureAtlasSprite ringCorner = spriteGetter.apply(RING_CORNER);
        TextureAtlasSprite ringSideHor = spriteGetter.apply(RING_SIDE_HOR);
        TextureAtlasSprite ringSideVer = spriteGetter.apply(RING_SIDE_VER);

        return new LightBakedModel(ringCorner, ringSideHor, ringSideVer,
            spriteGetter.apply(LIGHT_BASE), this.getLightMaterial(spriteGetter)) {
            public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand,
                ModelData data) {
                return CUTOUT;
            }
        };
        }

    public TextureAtlasSprite getLightMaterial(
        Function<Material, TextureAtlasSprite> textureGetter) {
        return switch (this.type) {
        case ACCELERATOR_4x -> textureGetter.apply(ACCELERATOR_4x_LIGHT);
        case ACCELERATOR_16x -> textureGetter.apply(ACCELERATOR_16x_LIGHT);
        case ACCELERATOR_64x -> textureGetter.apply(ACCELERATOR_64x_LIGHT);
        case ACCELERATOR_256x -> textureGetter.apply(ACCELERATOR_256x_LIGHT);
        default -> throw new IllegalArgumentException(
            "Crafting unit type " + this.type + " does not use a light texture.");
        };
    }

    private static Material texture(String namespace, String name) {
        var mat = new Material(TextureAtlas.LOCATION_BLOCKS,
            new ResourceLocation(namespace, "block/crafting/" + name));
        MATERIALS.add(mat);
        return mat;
    }

}

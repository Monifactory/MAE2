package appeng.client.render.cablebus;

import java.util.Map;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class TintedCableBusBakedModel extends CableBusBakedModel {
  TintedCableBusBakedModel(TintedCableBuilder cableBuilder,
    FacadeBuilder facadeBuilder, Map<ResourceLocation, BakedModel> partModels,
    TextureAtlasSprite particleTexture) {
    super(cableBuilder, facadeBuilder, partModels, particleTexture);
  }

}

package appeng.client.render.cablebus;

import java.util.function.Function;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;

public class TintedCableBuilder extends CableBuilder {

  TintedCableBuilder(
    Function<Material, TextureAtlasSprite> bakedTextureGetter) {
    super(bakedTextureGetter);
  }

}

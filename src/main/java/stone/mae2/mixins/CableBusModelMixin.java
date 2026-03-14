package stone.mae2.mixins;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.client.render.cablebus.CableBuilder;
import appeng.client.render.cablebus.CableBusModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import stone.mae2.MAE2;
import stone.mae2.client.render.model.TintedCableBuilder;

@Mixin(value = CableBusModel.class, remap = false)
public abstract class CableBusModelMixin {
  @Redirect(method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At(value = "NEW", target = "Lappeng/client/render/cablebus/CableBuilder;<init>(Ljava/util/function/Function;)Lappeng/client/render/cablebus/CableBuilder;"))
  public CableBuilder onCableBuilderConstructor(Function<Material, TextureAtlasSprite> spriteGetter) {
    MAE2.LOGGER.info("In CableBusModelMixin!");
    return new TintedCableBuilder(spriteGetter);
  }
}

package stone.mae2.mixins;

import appeng.api.config.BlockingMode;
import appeng.helpers.patternprovider.PatternProviderLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic;

@Mixin(value = PatternProviderLogic.class, remap = false, priority = 2000)
public abstract class PatternProviderLogicCLMixin {
    @Shadow
    public abstract BlockingMode getBlockingMode();

    @Inject(method = "pushPattern", at = @At("HEAD"))
    public void onPushStart(CallbackInfoReturnable<Boolean> cir) {
        PatternP2PTunnelLogic.blockingMode = this.getBlockingMode();
    }
    @Inject(method = "pushPattern", at = @At("TAIL"))
    public void onPushEnd(CallbackInfoReturnable<Boolean> cir) {
        PatternP2PTunnelLogic.blockingMode = BlockingMode.DEFAULT;
    }
}

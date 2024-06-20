package stone.mae2.mixins;

import appeng.api.parts.IPartItem;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import stone.mae2.api.features.MultiP2PTunnelAttunement;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = P2PTunnelPart.class, remap = false)
public abstract class P2PTunnelPartMixin extends AEBasePart {
    public P2PTunnelPartMixin(IPartItem<?> partItem) {
        super(partItem);
    }
    @Shadow
    private static final String CONFIG_NBT_FREQ = "p2pFreq";
    
    @Redirect(method = "onPartActivate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    public ItemStack getItemInHand(Player player, InteractionHand hand) {
        return player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
    }

    @Inject(method = "importSettings", at = @At(ordinal = 0, value = "INVOKE", target = "setOutput", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void afterSetOutput(SettingsFrom mode, CompoundTag input, @Nullable Player player, CallbackInfo ci) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            if (player != null) {
                if (player.getMainHandItem().isEmpty()) {
                    setOutput(false);
                    return;
                }
            }
        }
        setOutput(true);
    }

    @Redirect(method = "importSettings", at = @At(ordinal = 0, value = "INVOKE", target = "setOutput"))
    public void onSetOutput(P2PTunnelPart<?> thisPart, boolean isOutput) {}

    @Shadow
    abstract void setOutput(boolean output);
    @Shadow
    public abstract void setFrequency(short freq);
    @Shadow
    public abstract void onTunnelNetworkChange();
}

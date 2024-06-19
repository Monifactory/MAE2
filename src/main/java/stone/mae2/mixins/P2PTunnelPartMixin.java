package stone.mae2.mixins;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.parts.IPartItem;
import appeng.me.service.P2PService;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import stone.mae2.MAE2;
import stone.mae2.api.features.MultiP2PTunnelAttunement;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = P2PTunnelPart.class, remap = false)
public abstract class P2PTunnelPartMixin extends AEBasePart {
    public P2PTunnelPartMixin(IPartItem<?> partItem) {
        super(partItem);
    }

    @Shadow
    private short freq;

    @Shadow
    private static final String CONFIG_NBT_FREQ = null;
    
    @Redirect(method = "onPartActivate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    public ItemStack getItemInHand(Player player, InteractionHand hand) {
        return player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
    }

    @Inject(method = "onPartActivate", at = @At(ordinal = 0, value = "INVOKE", target = "importSettings"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void onUpdateFreq(Player player, InteractionHand hand, Vec3 pos, CallbackInfoReturnable<Boolean> ci, IMemoryCard mc, CompoundTag configData) {
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        ci.setReturnValue(false);
        boolean isInput = player.getMainHandItem().isEmpty()
                && player.getOffhandItem().getItem() instanceof IMemoryCard;
        if (!isInput)
            return;
        //System.out.println("onImportSettings");
        IGrid grid = getMainNode().getGrid();
        P2PService service = P2PService.get(grid);
        short freq = configData.getShort(CONFIG_NBT_FREQ);
        P2PTunnelPart<?> input = service.getInput(freq);
        if (input != null) {
            //System.out.println("Cancelling importSettings");
            if (input.getClass().isInstance(this)) {
                this.setOutput(false);
                service.updateFreq((P2PTunnelPart<?>) ((Object) this), freq);
                ci.setReturnValue(true);
            } else {
                ci.setReturnValue(false);
            }
        } else {
            
        }
    }

    //@Inject(method = "onPartActivate", at = @At(value = "INVOKE", target = "importSettings"), cancellable = true)
    //public void onUpdateFreq(CallbackInfoReturnable<Boolean> ci) {
    //    ci.setReturnValue(false);
    //}

    @Shadow
    abstract void setOutput(boolean output);
}

package stone.mae2.mixins;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.parts.IPart;
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

import java.util.Objects;

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
    private static final String CONFIG_NBT_FREQ = "p2pFreq";
    
    @Redirect(method = "onPartActivate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    public ItemStack getItemInHand(Player player, InteractionHand hand) {
        return player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
    }

    @Inject(method = "onPartActivate", at = @At(ordinal = 0, value = "INVOKE", target = "importSettings", shift = At.Shift.BEFORE), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void onUpdateFreq(Player player, InteractionHand hand, Vec3 pos, CallbackInfoReturnable<Boolean> ci, ItemStack is, IMemoryCard mc, CompoundTag configData, IPartItem<P2PTunnelPart<?>> item, IPart part ,P2PTunnelPart<?> newTunnel) {
        System.out.println("onImportSettings");
        boolean isInput = player.getMainHandItem().isEmpty()
                && player.getOffhandItem().getItem() instanceof IMemoryCard;
        if (!isInput)
            return;
        System.out.println("Cancelling importSettings");
        IGrid grid = getMainNode().getGrid();
        short freq = configData.getShort(CONFIG_NBT_FREQ);
        System.out.println(configData);
        if (grid == null) {
            System.out.println("Null grid");
            newTunnel.setFrequency(freq);
            ci.setReturnValue(true);
            return;
        }
        P2PService service = P2PService.get(grid);
        P2PTunnelPart<?> input = service.getInput(freq);
        System.out.println("Input: "+ Objects.toString(input));
        System.out.println("NewTunnel: "+ newTunnel.toString());
        System.out.println("this: "+ this.toString());
        if (input != null) {
            if (input.getClass().isInstance(newTunnel)) {
                System.out.println("Found preexisting single input");
                ci.setReturnValue(false);
            } else {
                System.out.println("Found preexisting single input of different type");
                ci.setReturnValue(false);
            }
            } else {
            System.out.println("setting single input");
            //newTunnel.setOutput(false);
            service.updateFreq(newTunnel, freq);
            //onTunnelNetworkChange();
            ci.setReturnValue(true);
            }
    }

    @Shadow
    abstract void setOutput(boolean output);
    @Shadow
    public abstract void setFrequency(short freq);
    @Shadow
    public abstract void onTunnelNetworkChange();
}

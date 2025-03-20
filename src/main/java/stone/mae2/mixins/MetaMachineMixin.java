package stone.mae2.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.common.machine.electric.TransformerMachine;

import stone.mae2.integration.GregTechIntegration;

@Mixin(value = MetaMachine.class, remap = false)
public abstract class MetaMachineMixin {
    @Shadow
    @Final
    public List<MachineTrait> traits;
    
    @Inject(method = "getTraits", at = @At("HEAD"), cancellable = true)
    void redirectGetTraits(CallbackInfoReturnable<List<MachineTrait>> cir) {
        if (GregTechIntegration.inEUP2P) {
            if ((Object) this instanceof TransformerMachine || (Object) this instanceof BatteryBufferMachine) {
            ((TieredEnergyMachine)((Object)this)).doExplosion(10);
            }
        }
    }
}

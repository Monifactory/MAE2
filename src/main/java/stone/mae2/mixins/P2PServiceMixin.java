package stone.mae2.mixins;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Multimap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.networking.IGrid;
import appeng.me.service.P2PService;
import appeng.parts.p2p.P2PTunnelPart;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.Shadow;
import stone.mae2.MAE2;
import stone.mae2.me.service.MultiP2PService;

@Mixin(value = P2PService.class, remap = false)
public abstract class P2PServiceMixin {

    @Shadow
    @Final
    private IGrid myGrid;
    
    @SuppressWarnings({"rawtypes", "unchecked" })
    @Redirect(method = "addNode", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Multimap;put"))
    public boolean onMapPutOutput(Multimap map, Object freq, Object single) {
        return onOutput((Multimap<Short, P2PTunnelPart<?>>) map, (Short) freq, (P2PTunnelPart<?>) single);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "addNode", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;put"))
    public Object onMapPutInput( HashMap map, Object freq, Object single) {
        return onInput((Map<Short, P2PTunnelPart<?>>)map, (Short)freq, (P2PTunnelPart<?>)single);
    }

    public Object onInput(Map<Short, P2PTunnelPart<?>> map, short freq, P2PTunnelPart<?> single) {
        // TODO check that this isn't the second input on the singles
        // TODO check that this frequency isn't already in the multiples
        return map.put(freq, single);
    }

    public boolean onOutput(Multimap<Short, P2PTunnelPart<?>> map, short freq, P2PTunnelPart<?> single) {
        // TODO check that this frequency isn't already in the multiples
        return false;
    }
}

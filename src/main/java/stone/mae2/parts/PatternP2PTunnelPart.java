package stone.mae2.parts;

import stone.mae2.MAE2;

import net.minecraft.resources.ResourceLocation;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;

import java.util.List;

public class PatternP2PTunnelPart
    extends P2PTunnelPart<PatternP2PTunnelPart> {

    private static final P2PModels MODELS = new P2PModels(
        new ResourceLocation(MAE2.MODID, "part/p2p/p2p_tunnel_pattern"));

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }
    public PatternP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
    }

}

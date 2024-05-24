package stone.mae2.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import stone.mae2.MAE2;

import java.util.ArrayList;
import java.util.List;

public class PatternP2PTunnelPart
    extends P2PTunnelPart<PatternP2PTunnelPart> {

    private static final P2PModels MODELS = new P2PModels(
        new ResourceLocation(MAE2.MODID, "part/p2p/p2p_tunnel_pattern"));

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    public PatternP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
    }
    
    public List<TunneledPos> getTunneledPositions() {
        if (this.isOutput())
        {
            PatternP2PTunnelPart input = this.getInput();
            Direction inputSide = input.getSide();
            return List.of(new TunneledPos(
                input.getBlockEntity().getBlockPos().relative(inputSide),
                inputSide.getOpposite()));
        } else
        {
            List<TunneledPos> outputs = new ArrayList<>();
            for (PatternP2PTunnelPart output : this.getOutputs())
            {
                Direction outputSide = output.getSide();
                outputs.add(new TunneledPos(
                    output.getBlockEntity().getBlockPos().relative(outputSide),
                    outputSide.getOpposite()));
            }
            return outputs;
        }
    }

    public record TunneledPos(BlockPos pos, Direction dir) {
    }

}

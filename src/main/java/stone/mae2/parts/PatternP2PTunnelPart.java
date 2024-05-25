package stone.mae2.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

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
            return null;
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

    private TunneledPos getInputPos() {
        PatternP2PTunnelPart input = this.getInput();
        if (input == null)
            return null;
        Direction inputSide = input.getSide();
        return new TunneledPos(
            input.getBlockEntity().getBlockPos().relative(inputSide),
            inputSide.getOpposite());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability) {
        if (this.isOutput())
        {
            TunneledPos provider = this.getInputPos();
            if (provider == null)
                return LazyOptional.empty();
            return this.getLevel().getBlockEntity(provider.pos())
                .getCapability(capability, provider.dir());
        } else
        {
            return LazyOptional.empty();
        }
    }

    public record TunneledPos(BlockPos pos, Direction dir) {

        private static final String POSITION = "mae2Pos";
        private static final String DIRECTION = "mae2Direction";

        public void writeToNBT(CompoundTag tag) {
            tag.putLong(POSITION, pos.asLong());
            tag.putByte(DIRECTION, (byte) dir.get3DDataValue());
        }

        public static TunneledPos readFromNBT(CompoundTag tag) {
            return new TunneledPos(BlockPos.of(tag.getLong(POSITION)),
                Direction.from3DDataValue(tag.getByte(DIRECTION)));
        }
    }

}

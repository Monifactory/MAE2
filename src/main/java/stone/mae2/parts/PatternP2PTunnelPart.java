package stone.mae2.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import stone.mae2.MAE2;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class PatternP2PTunnelPart extends P2PTunnelPart<PatternP2PTunnelPart> {

    private static final P2PModels MODELS = new P2PModels(
        new ResourceLocation(MAE2.MODID, "part/p2p/p2p_tunnel_pattern"));

    private PatternProviderTargetCache cache;

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

    @Override
    public void addToWorld() {
        super.addToWorld();
        Level level = this.getBlockEntity().getLevel();
        if (!level.isClientSide)
        {
            this.cache = new PatternProviderTargetCache((ServerLevel) level,
                this.getBlockEntity().getBlockPos().relative(this.getSide()),
                this.getSide().getOpposite(),
                new MachineSource(this.getMainNode()::getNode));
        }
    }

    @Nullable
    public List<TunneledPatternProviderTarget> getTargets() {
        if (this.isOutput())
        // you can't go through a output tunnel (duh)
        {
            return null;
        }

        return this.getOutputStream()
            .map((output) -> new TunneledPatternProviderTarget(
                output.getTarget(),
                new TunneledPos(output.getBlockEntity().getBlockPos()
                    .relative(output.getSide()), output.getSide())))
            .filter((target) -> target.target() != null).toList();
    }

    @Nullable
    private PatternProviderTargetCache getTarget() {
        return cache;
    }

    @Nullable
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
            BlockEntity providerEntity = this.getLevel()
                .getBlockEntity(provider.pos());
            if (providerEntity != null)
            {
                return providerEntity.getCapability(capability, provider.dir());
            } else
            {
                return LazyOptional.empty();
            }
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

    /**
     * A holder for a pattern provider target through a pattern P2P
     * 
     * if target is null it means this is actually for the default blocks around the
     * provider (ie its not tunneled and should act like normal)
     */
    public record TunneledPatternProviderTarget(PatternProviderTargetCache target,
        TunneledPos pos) {
    }

}

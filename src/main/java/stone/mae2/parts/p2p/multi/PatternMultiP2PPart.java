package stone.mae2.parts.p2p.multi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.p2p.P2PModels;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import stone.mae2.MAE2;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;
import stone.mae2.parts.p2p.PatternP2PTunnel;

public class PatternMultiP2PPart extends MultiP2PTunnelPart<PatternMultiP2PPart> implements PatternP2PTunnel {
    private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/multi_p2p_tunnel_pattern"));
    
    private PatternProviderTargetCache cache;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    public PatternMultiP2PPart(IPartItem<?> partItem) {
        super(partItem);

    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        Level level = this.getBlockEntity().getLevel();
        if (!level.isClientSide) {
            this.cache = new PatternProviderTargetCache((ServerLevel) level,
                    this.getBlockEntity().getBlockPos().relative(this.getSide()),
                    this.getSide().getOpposite(),
                    new MachineSource(this.getMainNode()::getNode));
        }
    }

    @Nonnull
    @Override
    public Stream<TunneledPatternProviderTarget> getTargets() {
        if (this.isOutput())
        // you can't go through a output tunnel (duh)
        {
            return Stream.empty();
        } else {

        return this.getOutputStream()
                .map((output) -> new TunneledPatternProviderTarget(
                        output.getTarget(),
                        new TunneledPos(output.getBlockEntity().getBlockPos()
                                .relative(output.getSide()), output.getSide())))
                .filter((target) -> target.target() != null);
        }
    }

    @Nullable
    private PatternProviderTargetCache getTarget() {
        return cache;
    }

    @Nullable
    @Override
    public Stream<TunneledPos> getTunneledPositions() {
        if (this.isOutput()) {
            return Stream.empty();
        } else {
            return this.getOutputStream().map(output -> {
                Direction outputSide = output.getSide();
                return new TunneledPos(
                        output.getBlockEntity().getBlockPos().relative(outputSide),
                        outputSide.getOpposite());
            });
        }
    }

    // TODO make the outputs link to every input, for the rare case 1 provider can't
    // keep up with it
    private TunneledPos getInputPos() {
        Optional<PatternMultiP2PPart> maybeInput = this.getInputStream().findAny();
        if (maybeInput.isEmpty()) {
            return null;
        } else {
            PatternMultiP2PPart input = maybeInput.get();
            Direction inputSide = input.getSide();
            return new TunneledPos(
                    input.getBlockEntity().getBlockPos().relative(inputSide),
                    inputSide.getOpposite());
        }
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
}

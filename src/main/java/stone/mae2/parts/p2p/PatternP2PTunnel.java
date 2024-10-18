package stone.mae2.parts.p2p;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;

public interface PatternP2PTunnel {
    @Nonnull
    public Stream<TunneledPatternProviderTarget> getTargets();

    @Nonnull
    public Stream<TunneledPos> getTunneledPositions();
    

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

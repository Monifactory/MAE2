package stone.mae2.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TrailSourceBlock extends Block {
    public TrailSourceBlock(Properties properties) { super(properties); }

    public TrailSourceBlock() { this(Properties.of()); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // if (random.nextFloat() < 1f)
        for (int i = 0; i < Math.abs(random.nextGaussian() * 4); i++)
        {
            Vec3 offset = new Vec3(pos.getX(), pos.getY(), pos.getZ());
            double polar = random.nextDouble() * 2 * Math.PI;
            double azimuthal = random.nextDouble() * 2 * Math.PI;
            Vec3 normal = new Vec3(Math.sin(polar) * Math.cos(azimuthal), Math.cos(polar),
                Math.sin(polar) * Math.sin(azimuthal));
            CloudChamberBlock
                .drawTrail(level, offset.offsetRandom(random, .5f).add(.5, .5, .5),
                normal.scale(CloudChamberBlock.AREA).add(offset).offsetRandom(random, CloudChamberBlock.AREA));
        }
    }
}

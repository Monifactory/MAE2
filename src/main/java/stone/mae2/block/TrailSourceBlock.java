package stone.mae2.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import stone.mae2.api.client.trails.CloudChamberUtil;

public class TrailSourceBlock extends Block {
  public TrailSourceBlock(Properties properties) { super(properties); }

  public TrailSourceBlock() { this(Properties.of()); }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos,
    RandomSource random) {
    // if (random.nextFloat() < 1f)
    for (int i = 0; i < Math.abs(random.nextGaussian() * 4); i++) {
      Vec3 offset = new Vec3(pos.getX(), pos.getY(), pos.getZ());
      Vec3 end = CloudChamberUtil.randomPoint(random, CloudChamberUtil.ALPHA);
      CloudChamberUtil
        .drawTrail(level, offset.offsetRandom(random, .5f).add(.5, .5, .5),
          end.add(offset), CloudChamberUtil.ALPHA);
    }
  }
}

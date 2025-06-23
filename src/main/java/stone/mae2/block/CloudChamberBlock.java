package stone.mae2.block;

import appeng.block.AEBaseBlock;
import appeng.decorative.solid.QuartzGlassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import stone.mae2.api.client.CloudChamberUtil;

public class CloudChamberBlock extends QuartzGlassBlock {

  public CloudChamberBlock(Properties properties) { super(properties); }

  public CloudChamberBlock() {
    this(AEBaseBlock
      .glassProps()
      .noOcclusion()
      .isValidSpawn((p1, p2, p3, p4) -> false)
      .lightLevel(($) -> 15));
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos,
    RandomSource random) {
    CloudChamberUtil.tryBackgroundRadiation(level, random);
  }
}

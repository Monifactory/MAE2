package stone.mae2.gametests;

import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import stone.mae2.MAE2;

@GameTestHolder(MAE2.MODID)
public class RedstoneP2P {

  private static final BlockPos LAMP_POS = new BlockPos(0, 2, 2);
  private static final BlockPos CELL_POS = new BlockPos(2, 2, 2);
  
  @GameTest(template = "multi/shared/simple")
  public static void multiLosePower(GameTestHelper helper) {
    BlockState state = helper.getBlockState(LAMP_POS);
    if (state.getOptionalValue(RedstoneLampBlock.LIT).orElse(false)) {
      helper.setBlock(CELL_POS, Blocks.AIR);
      helper.succeedWhen(() -> 
                         helper.assertFalse(helper.getBlockState(LAMP_POS).getValue(RedstoneLampBlock.LIT), "Redstone P2P failed to turn off after losing power"));
    }
  }

  @GameTest(template = "multi/shared/simple")
  public static void multiControllerConflict(GameTestHelper helper) {
    BlockState state = helper.getBlockState(LAMP_POS);
    if (state.getOptionalValue(RedstoneLampBlock.LIT).orElse(false)) {
      helper.setBlock(new BlockPos(1, 2, 2), AEBlocks.CONTROLLER.block());
      helper.setBlock(new BlockPos(1, 2, 0), AEBlocks.CONTROLLER.block());
      helper.succeedWhen(() -> 
                         helper.assertFalse(helper.getBlockState(LAMP_POS).getValue(RedstoneLampBlock.LIT), "Redstone P2P failed to turn off after controller conflict"));
    }
  }

  @GameTest(template = "multi/shared/simple")
  public static void multiBrokenCable(GameTestHelper helper) {
    BlockState state = helper.getBlockState(LAMP_POS);
    if (state.getOptionalValue(RedstoneLampBlock.LIT).orElse(false)) {
      helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
      helper.succeedWhen(() -> 
                         helper.assertFalse(helper.getBlockState(LAMP_POS).getValue(RedstoneLampBlock.LIT), "Redstone P2P failed to turn off after breaking cable"));
    }
  }
}

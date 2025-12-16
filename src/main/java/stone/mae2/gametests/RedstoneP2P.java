package stone.mae2.gametests;

import java.util.function.Consumer;

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
    helper.succeedWhen(new Helper("Redstone P2P failed to turn off after losing power", helper, (help) -> {
          help.setBlock(CELL_POS, Blocks.AIR);
    }));
  }

  @GameTest(template = "multi/shared/simple")
  public static void multiControllerConflict(GameTestHelper helper) {
    helper.succeedWhen(new Helper("Redstone P2P failed to turn off after controller conflict", helper, (help) -> {
          helper.setBlock(new BlockPos(1, 2, 2), AEBlocks.CONTROLLER.block());
          helper.setBlock(new BlockPos(1, 2, 0), AEBlocks.CONTROLLER.block());
    }));
  }

  @GameTest(template = "multi/shared/simple")
  public static void multiBrokenCable(GameTestHelper helper) {
    helper.succeedWhen(new Helper("Redstone P2P failed to turn off after breaking cable", helper, (help) -> {
          helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
    }));
  }

  @GameTest(template = "multi/shared/simple")
  public static void multiBrokenRedstone(GameTestHelper helper) {
    helper.succeedWhen(new Helper("Redstone P2P failed to turn off after breaking redstone block", helper, (help) -> {
          helper.setBlock(new BlockPos(2, 2, 0), Blocks.AIR);
    }));
  }

  public static class Helper implements Runnable {
    private final String message;
    private final GameTestHelper helper;
    private final Consumer<GameTestHelper> action;
    
    private boolean ran = false;

    public Helper(String message, GameTestHelper helper, Consumer<GameTestHelper> action) {
      this.message = message;
      this.helper = helper;
      this.action = action;
    }
    public void run() {
      BlockState state = helper.getBlockState(LAMP_POS);
      if (!ran) {
        if (state.getOptionalValue(RedstoneLampBlock.LIT).orElse(false)) {
          action.accept(helper);
          ran = true;
        }
        helper.fail("Redstone P2P failed to light the lamp");
      } else {
        helper.assertBlockProperty(LAMP_POS, RedstoneLampBlock.LIT, (value) -> value, message);
      }
    }
  }
}

package stone.mae2.gametests;

import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;

import stone.mae2.MAE2;

/**
 * Tests for pattern p2p behavior
 */
@GameTestHolder(MAE2.MODID)
public class PatternP2P {
  @GameTest(template = "single/multipart/crafting")
  public static void singleMultipartCrafting(GameTestHelper helper) {
    helper.setBlock(0, 2, 1, AEBlocks.CREATIVE_ENERGY_CELL.block());
    helper.succeedWhen(() -> {
      // no I don't know why assertContainerContains doesn't work
      BlockEntity barrel = helper.getBlockEntity(new BlockPos(1, 2, 0));
      barrel
        .getCapability(ForgeCapabilities.ITEM_HANDLER)
        .ifPresent(handler -> {
          helper
            .assertTrue(handler.getStackInSlot(0).is(Items.LADDER),
              "no ladder");
        });
    });
  }

  @GameTest(template = "single/fullblock/crafting")
  public static void singleFullblockCrafting(GameTestHelper helper) {
    helper.setBlock(0, 2, 1, AEBlocks.CREATIVE_ENERGY_CELL.block());
    helper.succeedWhen(() -> {
      // no I don't know why assertContainerContains doesn't work
      BlockEntity barrel = helper.getBlockEntity(new BlockPos(1, 2, 0));
      barrel
        .getCapability(ForgeCapabilities.ITEM_HANDLER)
        .ifPresent(handler -> {
          helper
            .assertTrue(handler.getStackInSlot(0).is(Items.LADDER),
              "no ladder");
        });
    });
  }
}

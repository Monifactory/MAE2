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
 * Tests to verify that vanilla mechanics still hold
 * 
 * For example, ensuring that pattern providers still work without a pattern
 * p2p.
 */
@GameTestHolder(MAE2.MODID)
public class Vanilla {
  @GameTest(template = "provider/fullblock/crafting")
  public static void providerFullblockCrafting(GameTestHelper helper) {
    helper.setBlock(1, 2, 0, AEBlocks.CREATIVE_ENERGY_CELL.block());
    helper.succeedWhen(() -> {
      // no I don't know why assertContainerContains doesn't work
      BlockEntity barrel = helper.getBlockEntity(new BlockPos(0, 1, 0));
      barrel
        .getCapability(ForgeCapabilities.ITEM_HANDLER)
        .ifPresent(handler -> {
          helper
            .assertTrue(handler.getStackInSlot(0).is(Items.LADDER),
              "no ladder");
        });
    });
  }

  @GameTest(template = "provider/multipart/crafting")
  public static void providerMultipartCrafting(GameTestHelper helper) {
    helper.setBlock(1, 2, 0, AEBlocks.CREATIVE_ENERGY_CELL.block());
    helper.succeedWhen(() -> {

      BlockEntity barrel = helper.getBlockEntity(new BlockPos(0, 1, 0));
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

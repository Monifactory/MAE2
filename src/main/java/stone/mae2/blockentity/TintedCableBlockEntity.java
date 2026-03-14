package stone.mae2.blockentity;

import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TintedCableBlockEntity extends CableBlockEntity {
  public TintedCableBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
    super(blockEntityType, pos, blockState);
  }
}

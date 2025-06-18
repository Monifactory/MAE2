package stone.mae2.blockentity;

import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TintedCableBusBlockEntity extends CableBusBlockEntity {
  public TintedCableBusBlockEntity(BlockEntityType<?> blockEntityType,
    BlockPos pos, BlockState blockState) {
    super(blockEntityType, pos, blockState);
  }

}

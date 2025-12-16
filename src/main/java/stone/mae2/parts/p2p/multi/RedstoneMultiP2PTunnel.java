package stone.mae2.parts.p2p.multi;

import java.util.List;
import java.util.Optional;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.util.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import stone.mae2.MAE2;

public class RedstoneMultiP2PTunnel extends MultiP2PTunnel<RedstoneMultiP2PTunnel, stone.mae2.parts.p2p.multi.RedstoneMultiP2PTunnel.Logic, stone.mae2.parts.p2p.multi.RedstoneMultiP2PTunnel.Part> {
  private int power;
  // short because I'm assuming people won't get more than 32767 parts in a single tunnel
  // no support for powers above 15 either, single p2p should handle them for that edge-case
  private final short[] powers = new short[16];

  public RedstoneMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
  }

  public void setPower(int power) {
    int oldPower = this.power;
    this.power = power;
    if (!updateOutputs())
      this.power = oldPower;
  }

  private boolean loop = false;

  /**
   * 
   * @return if it actually updated outputs (isn't in an update loop)
   */
  public boolean updateOutputs() {
    if (loop)
      return false;
    this.loop = true;
    for (var output : this.outputs) {
      ((RedstoneMultiP2PTunnel.Part) output.part).notifyNeighbors();
    }
    this.loop = false;
    return true;
  }

  private void changeInput(int newPower, int oldPower) {
    this.powers[oldPower]--;
    this.powers[newPower]++;

    if (oldPower >= this.power && this.powers[oldPower] <= 0)
      updatePower();
    else if (newPower > this.power)
      updatePower();
  }

  private void updatePower() {
    for (int i = 15; i > 0; i--) {
      if (this.powers[i] > 0) {
        this.setPower(i);
        return;
      }
    }
    this.setPower(0);
  }

  @Override
  protected void updateTunnels(boolean updateOutputs, boolean configChange) {
    super.updateTunnels(updateOutputs, configChange);
  }

  public class Logic extends MultiP2PTunnel<RedstoneMultiP2PTunnel, Logic, Part>.Logic {
    private int oldPower = 0;
    
    public Logic(Part part) {
      super(part);
    }

    public int getPower() {
      return power;
    }

    public void offerInput(int newPower) {
      if (newPower != oldPower) {
        RedstoneMultiP2PTunnel.this.changeInput(newPower, oldPower);
        oldPower = newPower;
      }
    }

    public void updateState() {
      if (this.part.isOutput()) {
        ((Part) this.part).notifyNeighbors();
      } else {
        this.offerInput(this.getPart().getPowerInput());
      }
    }

    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
      if (!this.getPart().isOutput())
        this.offerInput(this.getPart().getPowerInput());
    }
  }

  public static class Part extends MultiP2PTunnel.Part<RedstoneMultiP2PTunnel, Logic, Part> {

    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_redstone"));

    @PartModels
    public static List<IPartModel> getModels() {
      return MODELS.getModels();
    }

    public Part(IPartItem<?> partItem) {
      super(partItem);
    }

    @Override
    protected float getPowerDrainPerTick() {
      return 0.5f;
    }

    private void notifyNeighbors() {
      final Level level = this.getBlockEntity().getLevel();
      Platform.notifyBlocksOfNeighbors(level,
        this.getBlockEntity().getBlockPos());
      // and this cause sometimes it can go though walls.
      for (Direction face : Direction.values()) {
        Platform.notifyBlocksOfNeighbors(level,
          this.getBlockEntity().getBlockPos().relative(face));
      }
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
      super.onMainNodeStateChanged(reason);
      Optional<Logic> logic = this.getLogic();
      if (logic.isPresent())
        logic.get().updateState();
      else
        this.notifyNeighbors();
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos,
      BlockPos neighbor) {
      this.getLogic().ifPresent(logic -> logic.onNeighborChanged(level, pos, neighbor));
    }

    public int getPowerInput() {
      final BlockPos target = this.getBlockEntity().getBlockPos()
        .relative(this.getSide());

      final BlockState state = this.getBlockEntity().getLevel()
        .getBlockState(target);
      final Block b = state.getBlock();
      if (b != null) {
        Direction srcSide = this.getSide();
        // maybe make it only read wires if they point into it?
        // if (b instanceof RedStoneWireBlock) {
        // srcSide = Direction.UP;
        // }
        int power = state.getSignal(this.getLevel(), target, srcSide.getOpposite());
        return power;
      }
      return 0;
    }

    @Override
    public boolean canConnectRedstone() {
      return true;
    }

    @Override
    public int isProvidingStrongPower() {
      if (this.isOutput()) {
        if (this.getLogic().isPresent()) {
          int power = this.getLogic().get().getPower();
          return power;
        }
      }
      return 0;
    }

    @Override
    public int isProvidingWeakPower() {
      return this.isProvidingStrongPower();
    }

    @Override
    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public RedstoneMultiP2PTunnel createTunnel(short freq) {
      return new RedstoneMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<RedstoneMultiP2PTunnel> getTunnelClass() {
      return RedstoneMultiP2PTunnel.class;
    }
  }

  @Override
  public Logic createLogic(Part part) {
    return part.setLogic(new Logic(part));
  }
}

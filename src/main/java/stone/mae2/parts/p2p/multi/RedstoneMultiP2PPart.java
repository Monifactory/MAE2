package stone.mae2.parts.p2p.multi;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import stone.mae2.MAE2;
import appeng.api.networking.IGridNodeListener;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.RedstoneP2PTunnelPart;
import appeng.util.Platform;

public class RedstoneMultiP2PPart extends MultiP2PTunnelPart<RedstoneMultiP2PPart> {

    private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/multi_p2p_tunnel_redstone"));
    
    // just copying single for now
    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    private int power;
    private boolean recursive = false;

    public RedstoneMultiP2PPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    protected float getPowerDrainPerTick() {
        return 0.5f;
    }

    private void setNetworkReady() {
        if (this.isOutput()) {
            this.putInput(getInputPower());
        }
    }

    private void putInput(int newPower) {
        if (this.recursive) {
            return;
        }

        this.recursive = true;
        if (this.isOutput()) {
            if (this.getMainNode().isActive()) {
                if (this.power != newPower) {
                    this.power = newPower;
                    this.notifyNeighbors();
                }
            } else {
                if (this.power != 0) {
                    this.power = 0;
                    this.notifyNeighbors();
                }
            }
        }
        this.recursive = false;
    }

    private void notifyNeighbors() {
        final Level level = this.getBlockEntity().getLevel();

        Platform.notifyBlocksOfNeighbors(level, this.getBlockEntity().getBlockPos());

        // and this cause sometimes it can go thought walls.
        for (Direction face : Direction.values()) {
            Platform.notifyBlocksOfNeighbors(level, this.getBlockEntity().getBlockPos().relative(face));
        }
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        if (getMainNode().hasGridBooted()) {
            this.setNetworkReady();
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        this.power = tag.getInt("power");
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putInt("power", this.power);
    }

    @Override
    public void onTunnelNetworkChange() {
        this.setNetworkReady();
    }

    // TODO figure out better algorithm to prevent frivolous checks
    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        if (!this.isOutput()) {
            final BlockPos target = this.getBlockEntity().getBlockPos().relative(this.getSide());

            final BlockState state = this.getBlockEntity().getLevel().getBlockState(target);
            final Block b = state.getBlock();
            if (b != null) {
                Direction srcSide = this.getSide();
                if (b instanceof RedStoneWireBlock) {
                    srcSide = Direction.UP;
                }
                int oldInputPower = this.getInputPower();
                int oldPower = this.power;
                this.power = b.getSignal(state, this.getBlockEntity().getLevel(), target, srcSide);
                if (oldInputPower == oldPower && oldPower != this.power) {
                    int newInputPower = this.getInputPower();
                    if (newInputPower != oldInputPower) {
                        this.sendToOutput(newInputPower);
                    }
                }
                //this.power = Math.max(this.power,
                //b.getSignal(state, this.getBlockEntity().getLevel(), target, srcSide));
            } else {
            }
        }
    }

    public int getInputPower() {
        return this.getInputStream().mapToInt(in -> in.power).max().orElse(0);
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public int isProvidingStrongPower() {
        return this.isOutput() ? this.power : 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return this.isOutput() ? this.power : 0;
    }

    private void sendToOutput(int power) {
        for (RedstoneMultiP2PPart rs : this.getOutputs()) {
            rs.putInput(power);
        }
    }

    public int getPower() {
        return this.power;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

}

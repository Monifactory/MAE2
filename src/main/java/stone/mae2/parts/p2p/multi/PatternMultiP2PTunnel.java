package stone.mae2.parts.p2p.multi;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.capabilities.Capabilities;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.p2p.P2PModels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import stone.mae2.MAE2;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic.PatternP2PTunnel;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic.Target;

import java.util.ArrayList;
import java.util.List;

public class PatternMultiP2PTunnel extends
  MultiP2PTunnel<PatternMultiP2PTunnel, PatternMultiP2PTunnel.Logic, PatternMultiP2PTunnel.Part>
  implements PatternP2PTunnel {

  protected List<Logic> inputs;
  protected List<Logic> outputs;
  protected PatternP2PTunnelLogic logic;

  public PatternMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
    this.logic = new PatternP2PTunnelLogic(this);
  }

  @Override
  public Logic addTunnel(Part part) {
    Logic logic = super.addTunnel(part);
    if (part.isOutput()) {
      this.outputs.add(logic);
      this.logic.refreshOutputs();
    } else {
      this.inputs.add(logic);
      this.logic.refreshInputs();
    }
    return logic;
  }

  @Override
  public boolean removeTunnel(Part part) {
    if (part.isOutput()) {
      this.outputs.remove(part.logic);
      this.logic.refreshOutputs();
    } else {
      this.inputs.remove(part.logic);
      this.logic.refreshInputs();
    }
    return super.removeTunnel(part);
  }

  @Override
  public List<? extends Target> getPatternTunnelInputs() { return inputs; }

  @Override
  public List<? extends Target> getPatternTunnelOutputs() { return outputs; }

  @Override
  public Logic createLogic(Part part) { return part.setLogic(new Logic(part)); }

  public class Logic extends
    MultiP2PTunnel<PatternMultiP2PTunnel, PatternMultiP2PTunnel.Logic, PatternMultiP2PTunnel.Part>.Logic
    implements Target {

    public Logic(Part part) { super(part); }

    @Override
    public ServerLevel level() {
      return (ServerLevel) this.part.getBlockEntity().getLevel();
    }

    @Override
    public BlockPos pos() {
      return this.part.getBlockEntity().getBlockPos().relative(part.getSide());
    }

    @Override
    public Direction side() { return this.part.getSide().getOpposite(); }

    @Override
    public IActionSource source() { // TODO Auto-generated method stub
      return this.part.source;
    }
  }

  public static class Part extends
    MultiP2PTunnel.Part<PatternMultiP2PTunnel, PatternMultiP2PTunnel.Logic, PatternMultiP2PTunnel.Part> {
    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_pattern"));
    protected final IActionSource source;

    public Part(IPartItem<?> partItem) {
      super(partItem);
      this.source = new MachineSource(this);
    }

    @PartModels
    public static List<IPartModel> getModels() { return MODELS.getModels(); }

    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public PatternMultiP2PTunnel createTunnel(short freq) {
      return new PatternMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<PatternMultiP2PTunnel> getTunnelClass() {
      return PatternMultiP2PTunnel.class;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capabilityClass) {
      if (this.logic != null
        && capabilityClass == Capabilities.CRAFTING_MACHINE)
        return (LazyOptional<T>) LazyOptional.of(() -> this.getTunnel().logic);
      if (this.isOutput()) {
        List<? extends Target> inputList = this
          .getTunnel()
          .getPatternTunnelInputs();
        if (inputList.isEmpty())
          return null;
        Target provider = inputList.get(0);
        if (provider == null)
          return LazyOptional.empty();
        BlockEntity maybeEntity = this
          .getLevel()
          .getBlockEntity(provider.pos());
        if (maybeEntity != null) {
          if (maybeEntity instanceof ICraftingProvider
            || maybeEntity instanceof PatternProviderLogicHost) {
            return maybeEntity.getCapability(capabilityClass, provider.side());
          } else if (maybeEntity instanceof IPartHost host) {
            IPart maybePart = host.getPart(((Part) provider).getSide());
            if (maybePart != null && (maybePart instanceof ICraftingProvider
              || maybePart instanceof PatternProviderLogicHost)) {
              maybePart.getCapability(capabilityClass);
            }
          }
        }
      }
      return LazyOptional.empty();
    }
  }

}

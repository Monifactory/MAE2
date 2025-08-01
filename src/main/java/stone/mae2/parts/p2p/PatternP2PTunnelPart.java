package stone.mae2.parts.p2p;

import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.capabilities.Capabilities;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import stone.mae2.MAE2;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;
import stone.mae2.parts.p2p.PatternP2PPartLogic.PatternP2PPartLogicHost;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic.PatternP2PTunnel;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic.Target;
import stone.mae2.util.TransHelper;

import java.util.List;

public class PatternP2PTunnelPart extends P2PTunnelPart<PatternP2PTunnelPart>
  implements PatternP2PTunnel, PatternP2PPartLogicHost {
  private static final P2PModels MODELS = new P2PModels(
    new ResourceLocation(MAE2.MODID, "part/p2p/p2p_tunnel_pattern"));

  protected final IActionSource source;
  protected final LazyOptional<ICraftingMachine> logic;

  private PatternProviderTargetCache targetCache;
  private final PatternP2PPartLogic partLogic = new PatternP2PPartLogic(this);

  public PatternP2PTunnelPart(IPartItem<?> partItem) {
    super(partItem);
    this.source = new MachineSource(this);
    if (this.isOutput()) {
      this.logic = null;
    } else {
      this.logic = LazyOptional.of(() -> new PatternP2PTunnelLogic(this));
    }
  }

  @Override
  public void readFromNBT(CompoundTag data) {
    super.readFromNBT(data);
    this.partLogic.readFromNBT(data);
  }

  @Override
  public void writeToNBT(CompoundTag data) {
    super.writeToNBT(data);
    this.partLogic.writeToNBT(data);
  }

  @Override
  public void addToWorld() {
    super.addToWorld();
    this.targetCache = PatternP2PPartLogicHost.super.getCache();
  }

  public boolean isValid() { return this.partLogic.isValid(); }

  public void addToSendList(AEKey what, long l) {
    this.partLogic.addToSendList(what, l);
  }

  public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
    this.partLogic.addAdditionalDrops(drops, wrenched);
  }

  @Override
  public PatternProviderTargetCache getCache() { return this.targetCache; }

  private boolean isRecursive = false;

  @Override
  public PatternContainerGroup getGroup() {
    if (isRecursive) {
      return new PatternContainerGroup(AEItemKey.of(Items.BARRIER),
        TransHelper.GUI.translatable("patternP2P.recursive"), List.of());
    }
    try {
      isRecursive = true;
      PatternContainerGroup group = PatternP2PTunnel.super.getGroup();
      // always called on the input part anyways, no need to get it
      if (this.hasCustomName())
        return new PatternContainerGroup(group.icon(),
          TransHelper.GUI
            .translatable("patternP2P.aggregate", this.getCustomName(),
              this.getOutputs().size()),
          group.tooltip());
      else
        return group;
    } finally {
      isRecursive = false;
    }
  }

  @PartModels
  public static List<IPartModel> getModels() { return MODELS.getModels(); }

  public IPartModel getStaticModels() {
    return MODELS.getModel(this.isPowered(), this.isActive());
  }

  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> capabilityClass) {
    if (this.isActive() && this.getFrequency() != 0) {
      if (this.logic != null
        && capabilityClass == Capabilities.CRAFTING_MACHINE)
        return (LazyOptional<T>) logic;
      if (this.isOutput()) {
        PatternP2PTunnelPart provider = this.getInput();
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
            IPart maybePart = host.getPart(provider.getSide());
            if (maybePart != null && (maybePart instanceof ICraftingProvider
              || maybePart instanceof PatternProviderLogicHost)) {
              maybePart.getCapability(capabilityClass);
            }
          }
        }
      }
    }
    return LazyOptional.empty();
  }

  @Override
  public List<Target> getPatternTunnelInputs() {
    return List.of(this.getInput());
  }

  @Override
  public List<? extends Target> getPatternTunnelOutputs() {
    return this.getOutputs();
  }

  @Override
  public ServerLevel level() {
    // this has to be a server level
    // if it isn't there's nothing else to do really
    // returning null would just throw a NPE eventually
    return (ServerLevel) this.getLevel();
  }

  @Override
  public BlockPos pos() {
    return this.getBlockEntity().getBlockPos().relative(this.getSide());
  }

  @Override
  public Direction side() {
    return this.getSide().getOpposite();
  }

  @Override
  public IActionSource source() {
    return this.source;
  }
}
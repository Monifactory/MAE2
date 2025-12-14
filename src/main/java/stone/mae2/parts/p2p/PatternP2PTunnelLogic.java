package stone.mae2.parts.p2p;

import appeng.api.config.Actionable;
import appeng.api.config.BlockingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.helpers.patternprovider.PatternProviderTargetCache;
import appeng.util.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.checkerframework.checker.units.qual.C;

import stone.mae2.MAE2;
import stone.mae2.bootstrap.MAE2Items;
import stone.mae2.util.LoadedModsHelper;
import stone.mae2.util.TransHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternP2PTunnelLogic implements ICraftingMachine {

  protected final PatternP2PTunnel tunnel;
  protected PatternProviderTargetCache caches[];
  protected Set<AEKey> patternInputs;
  private int lastOutputIndex = 0;

  public static boolean isBlocking;
  public static BlockingMode blockingMode;

  public PatternP2PTunnelLogic(PatternP2PTunnel tunnel) {
    this.tunnel = tunnel;
    this.refreshInputs();
    this.refreshOutputs();
  }

  @Override
  public PatternContainerGroup getCraftingMachineInfo() {
    return tunnel.getGroup();
  }

  private boolean isRecursive;

  // copied/heavily inspired by AE2's {@link PatternProviderLogic#pushPattern}
  @Override
  public boolean pushPattern(IPatternDetails pattern, KeyCounter[] ingredients,
    Direction ejectionDirection) {
    if (isRecursive)
      return false;
    try {
      isRecursive = true;
      List<? extends Target> outputs = tunnel.getPatternTunnelOutputs();
      if (outputs.size() <= 0)
        return false;
      boolean isExternal = pattern.supportsPushInputsToExternalInventory();
      int i = lastOutputIndex;
      do {
        i = (i + 1) % outputs.size();
        Target output = outputs.get(i);
        if (!output.isValid())
          continue;
        ICraftingMachine craftingMachine = ICraftingMachine
          .of(output.level(), output.pos(), output.side(),
            output.level().getBlockEntity(output.pos()));
        if (craftingMachine != null && craftingMachine.acceptsPlans()) {
          if (craftingMachine
            .pushPattern(pattern, ingredients, output.side())) {
            lastOutputIndex = i;
            return true;
          }
          continue;
        }

          if (isExternal) {
              if (caches == null || caches.length != outputs.size()) {
                  refreshOutputs();
              }
              final PatternProviderTarget target = caches[i].find();
          if (target == null
            || shouldBlock(isBlocking, target, this.patternInputs))
            continue;
          if (targetAcceptsAll(target, ingredients)) {
            pattern
              .pushInputsToExternalInventory(ingredients, (what, amount) -> {
                var inserted = target.insert(what, amount, Actionable.MODULATE);
                if (inserted < amount) {
                  output.addToSendList(what, amount - inserted);
                }
              });
            lastOutputIndex = i;
            return true;
          }
        }
      } while (i != lastOutputIndex);
    } catch (Throwable t) {
        MAE2.LOGGER.error(t.getLocalizedMessage());
    } finally {
      isRecursive = false;
    }
    return false;
  }

  /**
   * Checks if the target should block more patterns being input or not
   * 
   * @param isBlocking whether or not the pushing provider was set to blocking
   * @param target     the target being input into
   * @param inputs     the pattern inputs that could be input into this target
   * @return whether or not further inputs should be blocked
   */
  public static boolean shouldBlock(boolean isBlocking,
    PatternProviderTarget target, Set<AEKey> inputs) {
    return isBlocking && (LoadedModsHelper.isFork
            ? target.containsPatternInput(inputs, blockingMode)
            : target.containsPatternInput(inputs));
  }

  // TODO make this more incremental instead of resetting everything on any
  // change
  public void refreshOutputs() {
    List<? extends Target> outputs = tunnel.getPatternTunnelOutputs();
    if (outputs.isEmpty()){
      this.caches = null;
      return;
    }
    this.caches = new PatternProviderTargetCache[outputs.size()];
    for (int i = 0; i < this.caches.length; i++) {
      Target output = outputs.get(i);
      this.caches[i] = output.getCache();
    }
    this.lastOutputIndex = this.lastOutputIndex % outputs.size();
  }

  // TODO make this more incremental instead of resetting everything on any
  // change
  public void refreshInputs() {
    if (this.isRecursive)
      return;
    try {
      this.isRecursive = true;
      this.patternInputs = new HashSet<>();
      for (Target input : tunnel.getPatternTunnelInputs()) {
        ICraftingProvider provider = input.getTargetCraftingProvider();
        if (provider == null) {
          ICraftingMachine maybeMachine = ICraftingMachine
            .of(input.getTargetBlockEntity(), input.side());
          if (maybeMachine != null
            && maybeMachine instanceof PatternP2PTunnelLogic inputLogic) {
            // technically a tunnel could connect to itself which will waste
            // some
            // time adding known duplicates, but it won't break anything and
            // there
            // shouldn't be a reason for players to do that in the first place
            this.patternInputs.addAll(inputLogic.patternInputs);
          }
          continue;
        }
        for (IPatternDetails pattern : provider.getAvailablePatterns()) {
          for (IInput ingredient : pattern.getInputs()) {
            for (GenericStack ingredientStack : ingredient
              .getPossibleInputs()) {
              this.patternInputs.add(ingredientStack.what().dropSecondary());
            }
          }
        }
      }

      for (Target output : tunnel.getPatternTunnelOutputs()) {
        ICraftingMachine maybeMachine = ICraftingMachine
          .of(output.getTargetBlockEntity(), output.side());
        if (maybeMachine != null
          && maybeMachine instanceof PatternP2PTunnelLogic outputLogic) {
          outputLogic.refreshInputs();
        }
      }
    } finally {
      this.isRecursive = false;
    }
  }

  public static boolean targetAcceptsAll(PatternProviderTarget target,
    KeyCounter[] inputHolder) {
    for (var inputList : inputHolder) {
      for (var input : inputList) {
        var inserted = target
          .insert(input.getKey(), input.getLongValue(), Actionable.SIMULATE);
        if (inserted == 0) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean acceptsPlans() {
    return true;
  }

  /**
   * @param level the level this target is in
   * @param pos   the block pos this target is targeting
   * @param side  the side this target is *coming* from (not the side of the
   *              part)
   */
  public interface Target {
    /**
     * the level this target is in
     */
    ServerLevel level();

    default PatternProviderTargetCache getCache() {
      if (LoadedModsHelper.isFork) {
        var provider = this.getTargetCraftingProvider();
        if (provider instanceof PatternProviderLogic ppLogic) {
          return new PatternProviderTargetCache(this.level(), this.pos(), this.side(), this.source(), (ConfigManager) ppLogic.getConfigManager());
        }
      }
      // This is valid in both cases, either as a fallback when the input is not a Pattern Provider,
      // or when we're not using the fork
      // Works fine because the fork has an alternate constructor,
      // so we don't need to use reflection, and we AT both those to public
      return new PatternProviderTargetCache(this.level(), this.pos(), this.side(), this.source());
    }

    boolean isValid();

    void addToSendList(AEKey what, long l);

    /**
     * the block pos this target is targeting
     */
    BlockPos pos();

    /**
     * the side this target is *coming* from (opposite the side of the part)
     */
    Direction side();

    IActionSource source();

    public default LazyOptional<C> getTargetCapability(Capability<C> cap) {
      BlockEntity be = level().getBlockEntity(pos());
      return be != null ? be.getCapability(cap, side()) : null;
    }

    public default BlockEntity getTargetBlockEntity() {
      return level().getBlockEntity(pos());
    }

    public default ICraftingProvider getTargetCraftingProvider() {
      BlockEntity maybeEntity = getTargetBlockEntity();
      if (maybeEntity != null) {
        if (maybeEntity instanceof ICraftingProvider provider) {
          return provider;
        } else if (maybeEntity instanceof PatternProviderLogicHost logicHost) {
          return logicHost.getLogic();
        } else if (maybeEntity instanceof IPartHost host) {
          IPart maybePart = host.getPart(side());
          if (maybePart != null) {
            if (maybePart instanceof ICraftingProvider provider) {
              return provider;
            } else if (maybePart instanceof PatternProviderLogicHost logicHost) {
              return logicHost.getLogic();
            }
          }
        }
      }
      return null;
    }
  }

  public static interface PatternP2PTunnel {
    boolean isRecursive = false;

    // Lists are needed so the tunnel can start from the part it left off at,
    // for a bit more tps
    List<? extends Target> getPatternTunnelInputs();

    List<? extends Target> getPatternTunnelOutputs();

    /**
     * Group of this tunnel
     * 
     * Override for custom names and can call the super method to default to
     * this name
     * 
     * @return group of this tunnel
     */
    default PatternContainerGroup getGroup() {
      List<? extends Target> outputs = getPatternTunnelOutputs();
      PatternContainerGroup firstGroup = null;
      int emptyCount = 0;
      int count = 0;
      boolean isMixed = false;
      for (Target output : outputs) {
        PatternContainerGroup newGroup = PatternContainerGroup
          .fromMachine(output.level(), output.pos(), output.side());
        if (newGroup == null) {
          emptyCount++;
          continue;
        }
        if (firstGroup == null)
          firstGroup = newGroup;
        if (!firstGroup.equals(newGroup))
          isMixed = true;
        count++;
      }
      if (firstGroup == null)
        return new PatternContainerGroup(
          AEItemKey.of(MAE2Items.PATTERN_P2P_TUNNEL.get()),
          TransHelper.GUI.translatable("patternP2P.nothing", emptyCount),
          List.of());
      if (isMixed)
        return new PatternContainerGroup(
          AEItemKey.of(MAE2Items.PATTERN_P2P_TUNNEL.get()),
          TransHelper.GUI.translatable("patternP2P.mixed", count), List.of());
      else
        return new PatternContainerGroup(firstGroup.icon(),
          TransHelper.GUI
            .translatable("patternP2P.aggregate", firstGroup.name(), count),
          firstGroup.tooltip());
    }
  }
}

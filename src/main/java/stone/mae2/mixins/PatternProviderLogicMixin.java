package stone.mae2.mixins;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.helpers.patternprovider.UnlockCraftingEvent;
import appeng.util.ConfigManager;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;
import stone.mae2.parts.PatternP2PTunnelPart;
import stone.mae2.parts.PatternP2PTunnelPart.TunneledPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Overwriting a method is terrible, but hopefully no one else is messing with
 * pattern providers
 */
@Mixin(value = PatternProviderLogic.class, remap = false)
public class PatternProviderLogicMixin {

    @Shadow
    private PatternProviderLogicHost host;
    @Shadow
    private IManagedGridNode mainNode;
    @Shadow
    private IActionSource actionSource;
    @Shadow
    private ConfigManager configManager;

    @Shadow
    private int priority;

    // Pattern storing logic
    @Shadow
    private AppEngInternalInventory patternInventory;
    @Shadow
    private List<IPatternDetails> patterns;
    /**
     * Keeps track of the inputs of all the patterns. When blocking mode is
     * enabled, if any of these is contained in the target, the pattern won't be
     * pushed. Always contains keys with the secondary component dropped.
     */
    @Shadow
    private Set<AEKey> patternInputs;
    // Pattern sending logic
    @Shadow
    private List<GenericStack> sendList;
    @Shadow
    private Direction sendDirection;
    // Stack returning logic
    @Shadow
    private PatternProviderReturnInventory returnInv;

    @Shadow
    private YesNo redstoneState;

    @Nullable
    @Shadow
    private UnlockCraftingEvent unlockEvent;
    @Nullable
    @Shadow
    private GenericStack unlockStack;
    @Shadow
    private int roundRobinIndex;
    private TunneledPos sendPos;

    /**
     * AE2's code is just not amenable to changes this radical, so I have to
     * overwrite it to allow multiple pattern targets per side. This is
     * potentially possible with finer grained overwrites (is that even
     * possible?) or asking AE2 to change their code to allow this
     * 
     * @param patternDetails
     * @param inputHolder
     * @return
     */
    @Overwrite
    public boolean pushPattern(IPatternDetails patternDetails,
        KeyCounter[] inputHolder) {
        if (!sendList.isEmpty() || !this.mainNode.isActive()
            || !this.patterns.contains(patternDetails))
        {
            return false;
        }

        var be = host.getBlockEntity();
        var level = be.getLevel();

        if (getCraftingLockedReason() != LockCraftingMode.NONE)
        {
            return false;
        }

        record PushTarget(Direction direction, PatternProviderTarget target) {
        }
        var possibleTargets = new ArrayList<PushTarget>();

        // use a boolean like this to allow multiple machines to pushed too a
        // tick
        // Push to crafting machines first
        for (Direction direction : getActiveSides())
        {
            Direction adjBeSide = direction.getOpposite();
            // Main change to allow multiple positions to be checked per side
            List<TunneledPos> positions = getTunneledPositions(
                be.getBlockPos().relative(direction), level, adjBeSide);
            rearrangeRoundRobin(positions);
            for (TunneledPos adjPos : positions)
            {
                BlockEntity adjBe = level.getBlockEntity(adjPos.pos());

                ICraftingMachine craftingMachine = ICraftingMachine.of(level,
                    adjPos.pos(),
                    adjPos.dir(), adjBe);
                if (craftingMachine != null && craftingMachine.acceptsPlans())
                {
                    if (craftingMachine.pushPattern(patternDetails, inputHolder,
                        adjPos.dir()))
                    {
                        onPushPatternSuccess(patternDetails);
                        // edit
                        return true;
                    }
                }



                // we found crafting machines, so there's nothing else to do
                // if (didSomething)
                // return true;

                // If no dedicated crafting machine could be found, and the
                // pattern does
                // not support
                // generic external inventories, stop here.
                if (!patternDetails.supportsPushInputsToExternalInventory())
                {
                    continue;
                }

                // find adapter had to be replaced with a pos using one instead
                var adapter = findAdapter(adjPos);
                if (adapter == null)
                    continue;

                if (this.isBlocking()
                    && adapter.containsPatternInput(this.patternInputs))
                {
                    continue;
                }

                if (this.adapterAcceptsAll(adapter, inputHolder))
                {
                    patternDetails.pushInputsToExternalInventory(inputHolder,
                        (what, amount) ->
                        {
                            var inserted = adapter.insert(what, amount,
                                Actionable.MODULATE);
                            if (inserted < amount)
                            {
                                this.addToSendList(what, amount - inserted);
                            }
                        });
                    onPushPatternSuccess(patternDetails);
                    this.sendPos = adjPos;
                    this.sendStacksOut(adapter);
                    ++roundRobinIndex;
                    return true;
                }
            }
        }

        // return didSomething;
        return false;
    }

    private boolean sendStacksOut(PatternProviderTarget adapter) {
        if (adapter == null)
        {
            return false;
        }

        for (var it = sendList.listIterator(); it.hasNext();)
        {
            var stack = it.next();
            var what = stack.what();
            long amount = stack.amount();

            long inserted = adapter.insert(what, amount, Actionable.MODULATE);
            if (inserted >= amount)
            {
                it.remove();
                return true;
            } else if (inserted > 0)
            {
                it.set(new GenericStack(what, amount - inserted));
                return true;
            }
        }

        if (sendList.isEmpty())
        {
            sendPos = null;
        }

        return false;
    }

    @Overwrite
    private boolean sendStacksOut() {
        if (sendPos == null)
        {
            if (!sendList.isEmpty())
            {
                throw new IllegalStateException(
                    "Invalid pattern provider state, this is a bug.");
            }
            return false;
        }

        boolean didSomething = false;

        if (sendStacksOut(findAdapter(sendPos)))
        {
            return true;
        }

        return didSomething;
    }

    private List<TunneledPos> getTunneledPositions(BlockPos pos, Level level,
        Direction adjBeSide) {
        BlockEntity potentialPart = level.getBlockEntity(pos);
        if (potentialPart == null || !(potentialPart instanceof IPartHost))
        {
            // can never tunnel
            return List.of(new TunneledPos(pos, adjBeSide));
        } else
        {
            IPart potentialTunnel = ((IPartHost) potentialPart)
                .getPart(adjBeSide);
            if (potentialTunnel instanceof PatternP2PTunnelPart)
            {
                return ((PatternP2PTunnelPart) potentialTunnel)
                    .getTunneledPositions();
            } else
            {
                // not a pattern p2p tunnel
                return List.of(new TunneledPos(pos, adjBeSide));
            }
        }
    }

    @Nullable
    private PatternProviderTarget findAdapter(TunneledPos pos) {
        var thisBe = host.getBlockEntity();
        return new PatternProviderTargetCache((ServerLevel) thisBe.getLevel(),
            pos.pos(), pos.dir(), actionSource).find();
    }

    private static final String SEND_POS_TAG = "sendPos";

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void onWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        if (sendPos != null)
        {
            CompoundTag sendPosTag = new CompoundTag();
            sendPos.writeToNBT(sendPosTag);
            tag.put(SEND_POS_TAG, sendPosTag);
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void onReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(SEND_POS_TAG))
        {
            sendPos = TunneledPos.readFromNBT(tag.getCompound(SEND_POS_TAG));
        }
    }
    @Shadow
    private <T> void rearrangeRoundRobin(List<T> list) {
        // TODO Auto-generated method stub

    }

    @Shadow
    private boolean isBlocking() {
        // TODO Auto-generated method stub
        return false;
    }

    @Shadow
    private boolean adapterAcceptsAll(PatternProviderTarget adapter,
        KeyCounter[] inputHolder) {
        // TODO Auto-generated method stub
        return false;
    }

    @Shadow
    private void addToSendList(AEKey what, long l) {
        // TODO Auto-generated method stub

    }

    @Shadow
    private void onPushPatternSuccess(IPatternDetails patternDetails) {
        // TODO Auto-generated method stub

    }

    @Shadow
    private Set<Direction> getActiveSides() {
        // TODO Auto-generated method stub
        return null;
    }

    @Shadow
    private LockCraftingMode getCraftingLockedReason() {
        return null;
    }
}

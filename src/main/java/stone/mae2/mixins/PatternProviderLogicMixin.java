/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see https://www.gnu.org/licenses/.
 */
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
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
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

import stone.mae2.MAE2;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;
import stone.mae2.parts.p2p.PatternP2PTunnel;
import stone.mae2.parts.p2p.PatternP2PTunnel.TunneledPatternProviderTarget;
import stone.mae2.parts.p2p.PatternP2PTunnel.TunneledPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Overwriting a method is terrible, but hopefully no one else is messing with
 * pattern providers
 */
@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {

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
     * Keeps track of the inputs of all the patterns. When blocking mode is enabled,
     * if any of these is contained in the target, the pattern won't be pushed.
     * Always contains keys with the secondary component dropped.
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

    private BlockPos sendPos;
    private PatternProviderTargetCache cache;

    /**
     * AE2's code is just not amenable to changes this radical, so I have to
     * overwrite it to allow multiple pattern targets per side. This is potentially
     * possible with finer grained overwrites (is that even possible?) or asking AE2
     * to change their code to allow this
     * 
     * @param patternDetails
     * @param inputHolder
     * @author Stone
     * @reason Had to rewrite it to be p2p aware, The original method just isn't
     *         flexible enough to do this with usual mixins
     * @return
     */
    @Overwrite
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
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
        for (Direction direction : getActiveSides())
            {

                Direction adjBeSide = direction.getOpposite();
                // Main change to allow multiple positions to be checked per
                // side
                List<TunneledPos> positions = getTunneledPositions(
                                                                   be.getBlockPos().relative(direction), level, adjBeSide);
                if (positions == null) {
                    continue;
                }
                for (TunneledPos adjPos : positions)
                    {
                        BlockEntity adjBe = level.getBlockEntity(adjPos.pos());

                        ICraftingMachine craftingMachine = ICraftingMachine.of(level, adjPos.pos(),
                                                                               adjPos.dir(), adjBe);
                        if (craftingMachine != null && craftingMachine.acceptsPlans())
                            {
                                if (craftingMachine.pushPattern(patternDetails, inputHolder, adjPos.dir()))
                                    {
                                        onPushPatternSuccess(patternDetails);
                                        // edit
                                        return true;
                                    }
                            }
                    }
            }
        if (patternDetails.supportsPushInputsToExternalInventory()) {
            // first gather up every adapter, to round robin out patterns
            List<TunneledPatternProviderTarget> adapters = new ArrayList<>();
            for (Direction direction : getActiveSides())
                {
                    findAdapters(be, level, adapters, direction);
                }
            rearrangeRoundRobin(adapters);

            for (TunneledPatternProviderTarget adapter : adapters)
                {
                    PatternProviderTargetCache targetCache = adapter.target();
                    PatternProviderTarget target = targetCache == null
                        ? findAdapter(adapter.pos().dir())
                        : targetCache.find();

                    if (target == null)
                        {
                            continue;
                        }
                    if (this.isBlocking() && target.containsPatternInput(this.patternInputs))
                        {
                            continue;
                        }

                    if (this.adapterAcceptsAll(target, inputHolder))
                        {
                            patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) ->
                                                                         {
                                                                             long inserted = target.insert(what, amount, Actionable.MODULATE);
                                                                             if (inserted < amount)
                                                                                 {
                                                                                     this.addToSendList(what, amount - inserted);
                                                                                 }
                                                                         });
                            onPushPatternSuccess(patternDetails);
                            this.sendPos = targetCache == null ? null : adapter.pos().pos();
                            this.sendDirection = adapter.pos().dir();
                            this.cache = targetCache;
                            // this.sendStacksOut(target);
                            ++roundRobinIndex;
                            return true;
                        }
                }
        }

        // return didSomething;
        return false;

    }

    private void findAdapters(BlockEntity be, Level level,
                              List<TunneledPatternProviderTarget> adapters, Direction direction) {
        BlockEntity potentialPart = level.getBlockEntity(be.getBlockPos().relative(direction));

        if (potentialPart == null || !(potentialPart instanceof IPartHost))
            {
                // no chance of tunneling
                adapters.add(new TunneledPatternProviderTarget(null,
                                                               new TunneledPos(be.getBlockPos(), direction)));
            } else
            {
                IPart potentialTunnel = ((IPartHost) potentialPart).getPart(direction.getOpposite());
                if (potentialTunnel != null && potentialTunnel instanceof PatternP2PTunnel)
                    {
                        List<TunneledPatternProviderTarget> newTargets = ((PatternP2PTunnel) potentialTunnel)
                            .getTargets();
                        if (newTargets != null)
                            {
                                adapters.addAll(newTargets);
                            }
                    } else
                    {
                        // not a pattern p2p tunnel
                        adapters.add(new TunneledPatternProviderTarget(null,
                                                                       new TunneledPos(be.getBlockPos(), direction)));
                    }
            }
    }

    /**
     * This code is mainly copied from the orginal sendStackOut() method. Mostly
     * just removed the code related to finding the adapter since it's passed in
     * now.
     * 
     * @param adapter
     * @return
     */
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

    /**
     * AE2 uses this method to send out ingredients that couldn't fit all at once
     * and have to be put in as space is made. I had to change it to use a cached
     * position found when the initial pattern was pushed. The position already has
     * gone through potential p2p tunnels.
     * 
     * @author Stone
     * @reason This method needs to be aware of the pattern p2p, and the original
     *         isn't flexible enough to allow that
     * @return true if it succeeded pushing out stacks
     */
    @Overwrite
    private boolean sendStacksOut() {
        if (sendDirection == null)
            {
                if (!sendList.isEmpty())
                    {
                        throw new IllegalStateException("Invalid pattern provider state, this is a bug.");
                    }
                return false;
            }

        if (cache == null)
            {
                if (this.sendPos == null)
                    {
                        return sendStacksOut(findAdapter(this.sendDirection));
                    } else
                    {
                        // when crafts are saved through a load, the cache won't exist but the send pos
                        // will
                        this.cache = findCache(sendPos, sendDirection);
                    }
            }
        return sendStacksOut(cache.find());
    }

    private List<TunneledPos> getTunneledPositions(BlockPos pos, Level level, Direction adjBeSide) {
        BlockEntity potentialPart = level.getBlockEntity(pos);
        if (potentialPart == null || !(potentialPart instanceof IPartHost))
            {
                // can never tunnel
                return List.of(new TunneledPos(pos, adjBeSide));
            } else
            {
                IPart potentialTunnel = ((IPartHost) potentialPart).getPart(adjBeSide);
                if (potentialTunnel instanceof PatternP2PTunnel tunnel)
                    {
                        return tunnel.getTunneledPositions();
                    } else
                    {
                        // not a pattern p2p tunnel
                        return List.of(new TunneledPos(pos, adjBeSide));
                    }
            }
    }

    @Nullable
    private PatternProviderTargetCache findCache(BlockPos pos, Direction dir) {
        var thisBe = host.getBlockEntity();
        return new PatternProviderTargetCache((ServerLevel) thisBe.getLevel(), pos, dir,
                                              actionSource);
    }

    @Shadow
    private PatternProviderTarget findAdapter(Direction side) {
        throw new RuntimeException("HOW, HOW DID YOU LOAD THIS!");
    };

    private static final String SEND_POS_TAG = "sendPos";

    /**
     * Writes the send position to nbt data
     * 
     * Writes the current send position to disk to allow it to persist through
     * unloads. Steals the vanilla sendDirection to determine from where it's
     * pushing to this postion. Currently this is why MAE2 can't be removed from a
     * save due to running crafts saving that they're crafting, but not where
     * they're crafting. I don't know if there's a clean way to do it (at least no
     * without incurring costs). Preferably I'd like it to be done without any costs
     * because the solution is just to stop all autocrafts before removing MAE2.
     * 
     * @param tag
     * @param ci
     */
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void onWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        if (sendPos != null)
            {
                tag.putLong(SEND_POS_TAG, sendPos.asLong());
            }
    }

    /**
     * Reads the send pos from the nbt data
     * 
     * Reads the saved data off the disk to reconstruct the pattern provider. Note
     * that this will also migrate old pattern providers from <1.2.0 MAE2 to
     * prevent crashes.
     * 
     * @param tag
     * @param ci
     */
    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void onReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(SEND_POS_TAG))
            // send pos only exists if MAE2 existed before
            {
                Tag sendPosTag = tag.get(SEND_POS_TAG);
                if (sendPosTag instanceof NumericTag numericTag) {
                    sendPos = BlockPos.of(numericTag.getAsLong());
                } else if (sendPosTag instanceof CompoundTag compoundTag) {
                    MAE2.LOGGER.debug("Migrated Pattern Provider from MAE2 1.2.0!");
                    TunneledPos tunnelPos = TunneledPos.readFromNBT(compoundTag);
                    this.sendPos = tunnelPos.pos();
                    this.sendDirection = tunnelPos.dir();
                }
            }
    }

    @Shadow
    private <T> void rearrangeRoundRobin(List<T> list) {}

    @Shadow
    public abstract boolean isBlocking();

    @Shadow
    private boolean adapterAcceptsAll(PatternProviderTarget adapter, KeyCounter[] inputHolder) {
        return false;
    }

    @Shadow
    private void addToSendList(AEKey what, long l) {}

    @Shadow
    private void onPushPatternSuccess(IPatternDetails patternDetails) {}

    @Shadow
    private Set<Direction> getActiveSides() {
        return null;
    }

    @Shadow
    public abstract LockCraftingMode getCraftingLockedReason();
}

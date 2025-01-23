/*
 * This file is part of MAE2.
 * Copyright (C) 2024 AE2 Enthusiast
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
package stone.mae2.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.execution.InputTemplate;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.UpgradeablePart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.srgutils.IMappingBuilder.IParameter;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;
import stone.mae2.parts.p2p.PatternP2PTunnel.TunneledPatternProviderTarget;
import stone.mae2.parts.p2p.PatternP2PTunnel.TunneledPos;

/*
 * A Pattern Bus is a part that can export an entire pattern atomicly following pattern p2ps and such.
 *
 * It works by extracting ingredients from the system and temporarily storing
 * them until there's space to export them. Additonally there's a system that
 * selects "concrete" types from the multiple types allowed per pattern
 * input. This way the part doesn't need to expend extra time every update
 * selecting the same thing.
 */
public class PatternBusPart extends UpgradeablePart implements IGridTickable, IViewCellStorage, InternalInventoryHost {
    protected IActionSource source;
    
    private final InternalInventory patternInventory;
    private final PatternProviderReturnInventory returnInventory;
    private final Collection<GenericStack> returnBuffer = new ArrayList<>();
    
    @Nullable
    private IPatternDetails pattern;

    /*
     * The current concrete ingredients needed for the pattern.
     *
     * null means no concrete types could be found last time, existing means
     * they were either found last time, or are currently extracted (in the
     * hasIngedients boolean)
     */
    private GenericStack[] currentIngredients;
    private boolean hasIngredients;

    public PatternBusPart(IPartItem<? extends PatternBusPart> partItem) {
        super(partItem);
        this.source = new MachineSource(this);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.patternInventory = new AppEngInternalInventory(this, 1);
        this.returnInventory = new PatternProviderReturnInventory(() -> {
                this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
                this.saveChanges();
        });
    }

    @Override
    public InternalInventory getViewCellStorage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getViewCellStorage'");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 40, isSleeping(), true);
    }
    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (isSleeping()) {
            return TickRateModulation.SLEEP;
        } else if (!canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        return this.doBusWork();
    }

    public TickRateModulation doBusWork() {
        if (!this.hasIngredients) {// check to see if new ingredients showed up
            if (this.currentIngredients == null) {
                this.currentIngredients = this.pickConcreteTypes();
                if (this.currentIngredients == null) {
                    return TickRateModulation.IDLE;
                }
            }
            this.hasIngredients = this.extractIngredients();
            if (hasIngredients) {
                // ingredients have already been pulled out, just need to try to
                // push the pattern
                this.pushPattern();
            } else {
                if (this.currentIngredients == null) {
                    this.currentIngredients = this.pickConcreteTypes();
                    if (this.currentIngredients == null) {
                        return TickRateModulation.IDLE;
                    }
                }
            }
        }
        return null;
    }

    private boolean pushPattern() {
        // This is copied direct from PatternProviderLogicMixin TODO make
        // pattern p2p tunneling into it's own method or something so we don't
        // have duplicated code like this
        Direction direction = this.getSide();
        BlockEntity be = this.getBlockEntity();
        Level level = this.getLevel();
        if (!this.pattern.supportsPushInputsToExternalInventory()) {

            Direction adjBeSide = direction.getOpposite();
            // Main change to allow multiple positions to be checked per
            // side
            List<TunneledPos> positions = getTunneledPositions(
                                                               be.getBlockPos().relative(direction), level, adjBeSide);
            if (positions == null) {
                continue;
            }
            for (TunneledPos adjPos : positions) {
                BlockEntity adjBe = level.getBlockEntity(adjPos.pos());

                ICraftingMachine craftingMachine = ICraftingMachine.of(level, adjPos.pos(),
                                                                       adjPos.dir(), adjBe);
                if (craftingMachine != null && craftingMachine.acceptsPlans())
                    {
                        if (craftingMachine.pushPattern(pattern, inputHolder, adjPos.dir()))
                            {
                                onPushPatternSuccess(patternDetails);
                                // edit
                                return true;
                            }
                    }
            }
        } else {
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
    }

    /**
     * Extracts a set of ingredients from storage
     *
     * While patterns can use tags and have multiple correct items per slot,
     * extracting from the network has to pick one concrete type to take
     * out. For performance, the concrete type to be extracted is stuck with
     * until the system runs out, then, new concretes are picked arbitrarily and
     * it continues.
     */
    private boolean extractIngredients() {
        long[] extracted = new long[this.currentIngredients.length];
        MEStorage storage = this.getGridNode().getGrid().getStorageService().getInventory();
        /*
         * while some of this could be reworked to maybe be more efficent when
         * running out of ingredients, the concrete type pick should have
         * ensured that we have items. Unless we have 1 of each tag's item in
         * storage, it should work more than it fails and be fine.
         */
        for (int i = 0; i < this.currentIngredients.length; i++) {
            GenericStack currentIngredient = this.currentIngredients[i];
            extracted[i] = storage.extract(currentIngredient.what(), currentIngredient.amount(), Actionable.MODULATE, this.source);
            if (extracted[i] != currentIngredient.amount()) {
                // not enough items to fulfill pattern
                // put everything into the return buffer, to be inserted into the system later
                for (int j = 0; j <= i; j++) {
                    this.returnBuffer.add(new GenericStack(this.currentIngredients[j].what(), extracted[j]));
                }
                return false;
            }
        }
        
        return true;
    }

    public GenericStack[] pickConcreteTypes() {
        KeyCounter cache = this.getGridNode().getGrid().getStorageService().getCachedInventory();
        IInput[] inputs = this.pattern.getInputs();
        GenericStack[] currentIngredients = new GenericStack[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            IInput input = inputs[i];
            AEKey concrete = null;
            long maxCount = Long.MIN_VALUE;
            // search through every possible input to this pattern (including fuzzy ones just in case)
            for (GenericStack possibleInput : input.getPossibleInputs()) {
                Collection<Object2LongMap.Entry<AEKey>> fuzzyCollection = cache.findFuzzy(possibleInput.what(), FuzzyMode.IGNORE_ALL);
                for (Object2LongMap.Entry<AEKey> fuzz : fuzzyCollection) {
                    AEKey type = fuzz.getKey();
                    long count = fuzz.getLongValue();
                    if (!input.isValid(type, this.getLevel()) || input.getMultiplier() >= count) {
                        continue;
                    }

                    if (count > maxCount) {
                        concrete = type;
                        maxCount = count;
                    }
                }
            }

            // nothing found for this ingredient, no valid ingredient set
            if (concrete == null) {
                return null;
            } else {
                currentIngredients[i] = new GenericStack(concrete, input.getMultiplier());
            }
        }
        return currentIngredients;
    }
    
    @Override
    public RedstoneMode getRSMode() {
        return this.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }

    public boolean canDoBusWork() {
        return this.pattern != null || !this.returnInventory.isEmpty();
    }
    // copied from export bus
    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(4, 4, 12, 12, 12, 14);
        bch.addBox(5, 5, 14, 11, 11, 15);
        bch.addBox(6, 6, 15, 10, 10, 16);
        bch.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    public void saveChanges() {
        this.getHost().markForSave();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        this.saveChanges();
        this.updatePattern();
    }

    private void updatePattern() {
        ItemStack patternStack = this.patternInventory.getStackInSlot(0);
        this.pattern = PatternDetailsHelper.decodePattern(patternStack, this.getLevel());
        this.currentIngredients = this.pickConcreteTypes();
    }
}

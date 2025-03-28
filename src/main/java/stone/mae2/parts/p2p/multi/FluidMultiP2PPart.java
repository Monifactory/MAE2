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
package stone.mae2.parts.p2p.multi;

import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import stone.mae2.MAE2;

import java.util.List;

public class FluidMultiP2PPart extends CapabilityMultiP2PPart<FluidMultiP2PPart, IFluidHandler> {

    private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/multi_p2p_tunnel_fluids"));
    private static final IFluidHandler NULL_FLUID_HANDLER = new NullFluidHandler();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public FluidMultiP2PPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.FLUID_HANDLER);
        inputHandler = new InputFluidHandler();
        outputHandler = new OutputFluidHandler();
        emptyHandler = NULL_FLUID_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int total = 0;

            final int outputTunnels = FluidMultiP2PPart.this.getOutputs().size();
            final int amount = resource.getAmount();

            if (outputTunnels == 0 || amount == 0) {
                return 0;
            }

            final int amountPerOutput = amount / outputTunnels;
            int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            for (FluidMultiP2PPart target : FluidMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IFluidHandler output = capabilityGuard.get();
                    final int toSend = amountPerOutput + overflow;
                    final FluidStack fillWithFluidStack = resource.copy();
                    fillWithFluidStack.setAmount(toSend);

                    final int received = output.fill(fillWithFluidStack, action);

                    overflow = toSend - received;
                    total += received;
                }
            }

            if (action == FluidAction.EXECUTE) {
                FluidMultiP2PPart.this.queueTunnelDrain(PowerUnits.FE,
                        (double) total / AEKeyType.fluids().getAmountPerOperation());
            }

            return total;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }

    }

    // TODO make this not have horrible performance
    // do the thing the ae2 uel item p2p does or whatever
    private class OutputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return FluidMultiP2PPart.this.getInputStream().mapToInt(input -> {
                    try (CapabilityGuard guard = input.getAdjacentCapability()) {
                        return guard.get().getTanks();
                    }
            }).sum();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            List<FluidMultiP2PPart> inputs = FluidMultiP2PPart.this.getInputs();
            int currentTank = tank;
            for (FluidMultiP2PPart input : inputs) {
                try (CapabilityGuard guard = input.getAdjacentCapability()) {
                    IFluidHandler handler = guard.get();
                    int slotCount = handler.getTanks();
                    if (currentTank < slotCount) {
                        return handler.getFluidInTank(currentTank);
                    } else {
                        currentTank -= slotCount;
                    }
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            List<FluidMultiP2PPart> inputs = FluidMultiP2PPart.this.getInputs();
            int currentTank = tank;
            for (FluidMultiP2PPart input : inputs) {
                try (CapabilityGuard guard = input.getAdjacentCapability()) {
                    IFluidHandler handler = guard.get();
                    int slotCount = handler.getTanks();
                    if (currentTank < slotCount) {
                        return handler.getTankCapacity(currentTank);
                    } else {
                        currentTank -= slotCount;
                    }
                }
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;

        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getAmount() == 0) {
                return FluidStack.EMPTY;
            }
            FluidStack toDrain = resource.copy();
            for (FluidMultiP2PPart target : FluidMultiP2PPart.this.getInputs()) {
                try (CapabilityGuard input = target.getAdjacentCapability()) {
                    FluidStack result = input.get().drain(resource, action);
                    toDrain.shrink(result.getAmount());
                    if (toDrain.getAmount() <= 0)
                        break;
                }
            }
            FluidStack drained = resource.copy();
            drained.shrink(toDrain.getAmount());
            if (action.execute()) {
                queueTunnelDrain(PowerUnits.FE,
                        (double) drained.getAmount() / AEKeyType.fluids().getAmountPerOperation());
            }

            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain == 0)
            {
                return FluidStack.EMPTY;
            }

            FluidStack potential = FluidStack.EMPTY;
            List<FluidMultiP2PPart> inputs = FluidMultiP2PPart.this.getInputs();
            for (FluidMultiP2PPart input : inputs) {
                try (CapabilityGuard guard = input.getAdjacentCapability()) {
                    potential = guard.get().drain(maxDrain, action);
                    if (potential != FluidStack.EMPTY)
                        break;
                }
            }

            FluidStack toDrain = potential.copy();
            toDrain.setAmount(maxDrain - potential.getAmount());
            return this.drain(toDrain, action);
        }
    }

    private static class NullFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

}

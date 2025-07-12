/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.parts.p2p.multi;

import java.util.List;
import java.util.Set;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import stone.mae2.MAE2;

public class FluidMultiP2PTunnel extends CapabilityMultiP2PTunnel<FluidMultiP2PTunnel, FluidMultiP2PTunnel.Logic, FluidMultiP2PTunnel.Part, IFluidHandler> {
  public FluidMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
    this.inputHandler = new InputHandler();
    this.outputHandler = new OutputHandler();
    this.emptyHandler = NULL_FLUID_HANDLER;
  }

  private static final IFluidHandler NULL_FLUID_HANDLER = new NullHandler();

  public class Logic extends CapabilityMultiP2PTunnel<FluidMultiP2PTunnel, Logic, Part, IFluidHandler>.Logic {
    public Logic(Part part) {
      super(part);
    }
  }

  public static class Part extends CapabilityMultiP2PTunnel.Part<FluidMultiP2PTunnel, Logic, Part, IFluidHandler> {
    public Part(IPartItem<?> partItem) {
      super(partItem);
    }

    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_fluids"));

    @PartModels
    public static List<IPartModel> getModels() {
      return MODELS.getModels();
    }

    @Override
    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public FluidMultiP2PTunnel createTunnel(short freq) {
      return new FluidMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<FluidMultiP2PTunnel> getTunnelClass() {
      return FluidMultiP2PTunnel.class;
    }
  }

  private class InputHandler implements IFluidHandler {
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

      final int outputTunnels = FluidMultiP2PTunnel.this.getOutputs().size();
      final int amount = resource.getAmount();

      if (outputTunnels == 0 || amount == 0) {
        return 0;
      }

      final int amountPerOutput = amount / outputTunnels;
      int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

      for (Logic output : FluidMultiP2PTunnel.this.getOutputs()) {
        try (var capabilityGuard = output.getAdjacentCapability()) {
          final IFluidHandler handler = capabilityGuard.get();
          final int toSend = amountPerOutput + overflow;
          final FluidStack fillWithFluidStack = resource.copy();
          fillWithFluidStack.setAmount(toSend);

          final int received = handler.fill(fillWithFluidStack, action);

          overflow = toSend - received;
          total += received;
        }
      }

      if (action == FluidAction.EXECUTE) {
        FluidMultiP2PTunnel.this.deductTransportCost(total, AEKeyType.fluids());
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
  private class OutputHandler implements IFluidHandler {
    @Override
    public int getTanks() {
      int tanks = 0;
      for (Logic input : getInputs()) {
        try (var guard = input.getAdjacentCapability()) {
          tanks += guard.get().getTanks();
        }
      }
      return tanks;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
      Set<Logic> inputs = FluidMultiP2PTunnel.this.getInputs();
      int currentTank = tank;
      for (Logic input : inputs) {
        try (var guard = input.getAdjacentCapability()) {
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
      Set<Logic> inputs = FluidMultiP2PTunnel.this.getInputs();
      int currentTank = tank;
      for (Logic input : inputs) {
        try (var guard = input.getAdjacentCapability()) {
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
      for (Logic input : FluidMultiP2PTunnel.this.getInputs()) {
        try (var guard = input.getAdjacentCapability()) {
          FluidStack result = guard.get().drain(resource, action);
          toDrain.shrink(result.getAmount());
          if (toDrain.getAmount() <= 0)
            break;
        }
      }
      FluidStack drained = resource.copy();
      drained.shrink(toDrain.getAmount());
      if (action.execute()) {
        FluidMultiP2PTunnel.this.deductTransportCost(drained.getAmount(),
          AEKeyType.fluids());
      }

      return drained;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
      if (maxDrain == 0) {
        return FluidStack.EMPTY;
      }

      FluidStack potential = FluidStack.EMPTY;
      Set<Logic> inputs = FluidMultiP2PTunnel.this.getInputs();
      for (Logic input : inputs) {
        try (var guard = input.getAdjacentCapability()) {
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

  private static class NullHandler implements IFluidHandler {

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

  @Override
  protected Capability<IFluidHandler> getCapability() {
    return ForgeCapabilities.FLUID_HANDLER;
  }

  @Override
  public Logic createLogic(Part part) {
    return part.setLogic(new Logic(part));
  }

}

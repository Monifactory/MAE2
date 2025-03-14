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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import stone.mae2.MAE2;

public class ItemMultiP2PTunnel extends CapabilityMultiP2PTunnel<ItemMultiP2PTunnel, ItemMultiP2PTunnel.Logic, ItemMultiP2PTunnel.Part, IItemHandler> {
  public ItemMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
    this.inputHandler = new InputHandler();
    this.outputHandler = new OutputHandler();
    this.emptyHandler = NULL_ITEM_HANDLER;
  }

  private static final IItemHandler NULL_ITEM_HANDLER = new NullHandler();

  public class Logic extends CapabilityMultiP2PTunnel<ItemMultiP2PTunnel, Logic, Part, IItemHandler>.Logic {
    public Logic(Part part) {
      super(part);
    }

  }

  public static class Part extends CapabilityMultiP2PTunnel.Part<ItemMultiP2PTunnel, Logic, Part, IItemHandler> {
    public Part(IPartItem<?> partItem) {
      super(partItem);
    }

    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_items"));

    @PartModels
    public static List<IPartModel> getModels() {
      return MODELS.getModels();
    }

    @Override
    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public ItemMultiP2PTunnel createTunnel(short freq) {
      return new ItemMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<ItemMultiP2PTunnel> getTunnelClass() {
      return ItemMultiP2PTunnel.class;
    }
  }

  private class InputHandler implements IItemHandler {

    @Override
    public int getSlots() {
      return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
      return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      int remainder = stack.getCount();

      final int outputTunnels = ItemMultiP2PTunnel.this.getOutputs().size();
      final int amount = stack.getCount();

      if (outputTunnels == 0 || amount == 0) {
        return stack;
      }

      final int amountPerOutput = amount / outputTunnels;
      int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

      for (Logic output : ItemMultiP2PTunnel.this.getOutputs()) {
        try (var capabilityGuard = output.getAdjacentCapability()) {
          final IItemHandler handler = capabilityGuard.get();
          final int toSend = amountPerOutput + overflow;

          if (toSend <= 0) {
            // Both overflow and amountPerOutput are 0, so they will be for
            // further outputs as well.
            break;
          }

          // So the documentation says that copying the stack should not be
          // necessary because it is not
          // supposed to be stored or modifed by insertItem. However,
          // ItemStackHandler will gladly store
          // the stack so we need to do a defensive copy. Forgecord says this is
          // the intended behavior,
          // and the documentation is wrong.
          ItemStack stackCopy = stack.copy();
          stackCopy.setCount(toSend);
          final int sent = toSend - ItemHandlerHelper
            .insertItem(handler, stackCopy, simulate).getCount();

          overflow = toSend - sent;
          remainder -= sent;
        }
      }

      if (!simulate) {
        ItemMultiP2PTunnel.this.deductTransportCost(amount - remainder,
          AEKeyType.items());
      }

      if (remainder == stack.getCount()) {
        return stack;
      } else if (remainder == 0) {
        return ItemStack.EMPTY;
      } else {
        ItemStack copy = stack.copy();
        copy.setCount(remainder);
        return copy;
      }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      return true;
    }

  }

  // TODO make this not completely terrible for performance
  private class OutputHandler implements IItemHandler {
    @Override
    public int getSlots() {
      int slots = 0;
      for (Logic input : ItemMultiP2PTunnel.this.getInputs()) {
        try (var guard = input.getAdjacentCapability()) {
          slots += guard.get().getSlots();
        }
      }
      return slots;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
      Set<Logic> inputs = ItemMultiP2PTunnel.this.getInputs();
      int currentSlot = slot;
      for (Logic input : inputs) {
        try (var guard = input.getAdjacentCapability()) {
          IItemHandler handler = guard.get();
          int slotCount = handler.getSlots();
          if (currentSlot < slotCount) {
            return handler.getStackInSlot(currentSlot);
          } else {
            currentSlot -= slotCount;
          }
        }
      }
      return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      Set<Logic> inputs = ItemMultiP2PTunnel.this.getInputs();
      int currentSlot = slot;
      for (Logic input : inputs) {
        try (var guard = input.getAdjacentCapability()) {
          IItemHandler handler = guard.get();
          int slotCount = handler.getSlots();
          if (currentSlot < slotCount) {
            return handler.extractItem(currentSlot, amount, simulate);
          } else {
            currentSlot -= slotCount;
          }
        }
      }
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      return false;
    }
  }

  private static class NullHandler implements IItemHandler {

    @Override
    public int getSlots() {
      return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
      return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      return false;
    }
  }

  @Override
  protected Capability<IItemHandler> getCapability() {
    return ForgeCapabilities.ITEM_HANDLER;
  }

  @Override
  public Logic createLogic(Part part) {
    return part.setLogic(new Logic(part));
  }
}

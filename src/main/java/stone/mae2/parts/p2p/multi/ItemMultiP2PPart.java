package stone.mae2.parts.p2p.multi;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import stone.mae2.MAE2;
import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;

public class ItemMultiP2PPart extends CapabilityMultiP2PPart<ItemMultiP2PPart, IItemHandler> {

    private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/multi_p2p_tunnel_items"));
    private static final IItemHandler NULL_ITEM_HANDLER = new NullItemHandler();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public ItemMultiP2PPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.ITEM_HANDLER);
        inputHandler = new InputItemHandler();
        outputHandler = new OutputItemHandler();
        emptyHandler = NULL_ITEM_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputItemHandler implements IItemHandler {

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

            final int outputTunnels = ItemMultiP2PPart.this.getOutputs().size();
            final int amount = stack.getCount();

            if (outputTunnels == 0 || amount == 0) {
                return stack;
            }

            final int amountPerOutput = amount / outputTunnels;
            int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            for (ItemMultiP2PPart target : ItemMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IItemHandler output = capabilityGuard.get();
                    final int toSend = amountPerOutput + overflow;

                    if (toSend <= 0) {
                        // Both overflow and amountPerOutput are 0, so they will be for further outputs as well.
                        break;
                    }

                    // So the documentation says that copying the stack should not be necessary because it is not
                    // supposed to be stored or modifed by insertItem. However, ItemStackHandler will gladly store
                    // the stack so we need to do a defensive copy. Forgecord says this is the intended behavior,
                    // and the documentation is wrong.
                    ItemStack stackCopy = stack.copy();
                    stackCopy.setCount(toSend);
                    final int sent = toSend - ItemHandlerHelper.insertItem(output, stackCopy, simulate).getCount();

                    overflow = toSend - sent;
                    remainder -= sent;
                }
            }

            if (!simulate) {
                ItemMultiP2PPart.this.queueTunnelDrain(PowerUnits.FE, amount - remainder);
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
    private class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return ItemMultiP2PPart.this.getInputStream().mapToInt(input -> {
                    try (CapabilityGuard guard = input.getAdjacentCapability()) {
                        return guard.get().getSlots();
                    }
                }).sum();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            List<ItemMultiP2PPart> inputs = ItemMultiP2PPart.this.getInputs();
            int currentSlot = slot;
            for (ItemMultiP2PPart input : inputs) {
                try (CapabilityGuard guard = input.getAdjacentCapability()) {
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
            List<ItemMultiP2PPart> inputs = ItemMultiP2PPart.this.getInputs();
            int currentSlot = slot;
            for (ItemMultiP2PPart input : inputs) {
                try (CapabilityGuard guard = input.getAdjacentCapability()) {
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

    private static class NullItemHandler implements IItemHandler {

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
}

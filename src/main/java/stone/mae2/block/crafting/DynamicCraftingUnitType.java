package stone.mae2.block.crafting;

import appeng.block.crafting.ICraftingUnitType;
import net.minecraft.world.item.Item;

import stone.mae2.core.MAE2Items;

public enum DynamicCraftingUnitType implements ICraftingUnitType {
    ACCELERATOR_4x(0, 4), ACCELERATOR_16x(0, 16), ACCELERATOR_64x(0, 64), ACCELERATOR_256x(0, 256),
    STORAGE_MAX(Long.MAX_VALUE, 0), ACCELERATOR_MAX(0, Integer.MAX_VALUE - 1);

    private final long storage;
    private final int threads;

    DynamicCraftingUnitType(long storage, int threads) {
        this.storage = storage;
        this.threads = threads;
    }

    @Override
    public long getStorageBytes() {
        return this.storage;
    }

    @Override
    public int getAcceleratorThreads() {
        return this.threads;
    }

    @Override
    public Item getItemFromType() {
        var item = switch (this) {
        case ACCELERATOR_4x -> MAE2Items.ACCELERATOR_4x;
        case ACCELERATOR_16x -> MAE2Items.ACCELERATOR_16x;
        case ACCELERATOR_64x -> MAE2Items.ACCELERATOR_64x;
        case ACCELERATOR_256x -> MAE2Items.ACCELERATOR_256x;
        case ACCELERATOR_MAX -> MAE2Items.ACCELERATOR_MAX;
        case STORAGE_MAX -> MAE2Items.STORAGE_MAX;
        };
        return item.get();
    }

}

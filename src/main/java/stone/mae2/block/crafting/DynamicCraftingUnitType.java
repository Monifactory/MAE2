package stone.mae2.block.crafting;

import appeng.block.crafting.ICraftingUnitType;
import net.minecraft.world.item.Item;

import stone.mae2.core.MAE2Items;

public class DynamicCraftingUnitType implements ICraftingUnitType {

    private final long storage;
    private final int threads;

    public DynamicCraftingUnitType(long storage, int threads) {
        this.storage = storage;
        this.threads = threads;
    }

    @Override
    public long getStorageBytes() {
        return this.storage;
    }

    private boolean flag = false;

    @Override
    public int getAcceleratorThreads() {
        if (flag)
        {
            flag = !flag;
            return this.threads;
        } else
        {
            flag = !flag;
            return 1;
        }
    }

    @Override
    public Item getItemFromType() {
        if (threads > 0)
        {
            switch (threads) {
            case 4:
                return MAE2Items.DENSE_ACCELERATORS[0].get();
            case 16:
                return MAE2Items.DENSE_ACCELERATORS[0].get();
            case 64:
                return MAE2Items.DENSE_ACCELERATORS[0].get();
            case 256:
                return MAE2Items.DENSE_ACCELERATORS[0].get();
            case Integer.MAX_VALUE:
                return MAE2Items.MAX_ACCELERATOR.get();
            }
        } else
        {
            return MAE2Items.MAX_STORAGE.get();
        }
        return null;
    }

}

package stone.mae2.blocks.crafting;

import appeng.block.crafting.ICraftingUnitType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * A Crafting Unit type that allows arbitrary stats
 */
public class DynamicCraftingUnitType implements ICraftingUnitType {

    private final RegistryObject<Item> item;
    private final long storage;
    private final int threads;

    public DynamicCraftingUnitType(RegistryObject<Item> item, long storage, int threads) {
        this.item = item;
        this.storage = storage;
        this.threads = threads;
    }

    @Override
    public String toString() {
        return String.format("Storage: %s, Threads: %s", this.storage, this.threads);
    }

    @Override
    public int getAcceleratorThreads() {
        return this.threads;
    }

    @Override
    public Item getItemFromType() {
        return this.item.get();
    }

    @Override
    public long getStorageBytes() {
        return this.storage;
    }

}

package stone.mae2.items;

import appeng.api.implementations.items.IStorageComponent;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DynamicStorageComponentItem extends Item
    implements IStorageComponent {
    /**
     * The tier of the storage component's units
     *
     * 0 is kilobytes, 1 is megabytes, 2 is gigabytes, etc.
     */
    private int prefix;
    /**
     * The tier of the storage component's amount itself
     *
     * 0 is 1, 1 is 4, 2 is 16, etc.
     */
    private int tier;
    private double bytes;

    public static Int2IntFunction colorSupplier = (prefix) -> 0xffff0000;

    public DynamicStorageComponentItem(Item.Properties properties, int prefixTier, int tier) {
        super(properties);
        this.prefix = prefixTier;
        this.tier = tier;
        this.bytes = (Math.pow(1024, prefixTier + 1) * Math.pow(4, tier));
    }

    public int getColor() {
        return colorSupplier.applyAsInt(prefix);
    }

    @Override
    public int getBytes(ItemStack is) {
        return (int) Math.min(bytes,
            Integer.MAX_VALUE / 8 - 1);
    }

    @Override
    public boolean isStorageComponent(ItemStack is) {
        return true;
    }
}

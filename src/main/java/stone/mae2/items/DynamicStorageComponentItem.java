package stone.mae2.items;

import appeng.items.materials.StorageComponentItem;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;

public class DynamicStorageComponentItem extends StorageComponentItem {
    /**
     * The tier of the storage component's units
     *
     * 0 is kilobytes, 1 is megabytes, 2 is gigabytes, etc.
     */
    private int prefixTier;
    /**
     * The tier of the storage component's amountt itself
     *
     * 0 is 1, 1 is 4, 2 is 16, etc.
     */
    private int tier;

    public DynamicStorageComponentItem(Item.Properties properties, int prefixTier, int tier) {
        super(properties, ((256 * prefixTier * 4) * (tier) * 4) + 1);
        this.prefixTier = prefixTier;
        this.tier = tier;
    }

    public int getColor() {
        return 1;
    }
}

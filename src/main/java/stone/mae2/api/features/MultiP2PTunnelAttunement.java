package stone.mae2.api.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public final class MultiP2PTunnelAttunement {
    private static final Map<Item, Item> single2MultiAttunements = new HashMap<>();

    public synchronized static Item getMultiPartBySinglePart(Item single) {
        return single2MultiAttunements.get(single);
    }

    public synchronized static void registerAttunementItem(ItemLike single, ItemLike multi) {
        Objects.requireNonNull(single.asItem(), "Single Tunnel version can't be null!");
        Objects.requireNonNull(multi.asItem(), "Multi Tunnel version can't  be null!");
        single2MultiAttunements.put(single.asItem(), multi.asItem());
    }
}

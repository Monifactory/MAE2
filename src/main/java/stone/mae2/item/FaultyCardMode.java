package stone.mae2.item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import appeng.api.implementations.items.IMemoryCard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import stone.mae2.MAE2;
import stone.mae2.item.faulty.AoEPaste;

public abstract class FaultyCardMode {    
    private static final Map<ResourceLocation, Function<CompoundTag, FaultyCardMode>> REGISTRY = new HashMap<>();
    private static final String MODE_TYPE = "type";

    public static FaultyCardMode of(CompoundTag tag) {
        var supplier = REGISTRY.get(new ResourceLocation(tag.getString(MODE_TYPE)));
        if (supplier == null) {
            return new AoEPaste();
        } else {
            return supplier.apply(tag);
        }
    }

    public static void register(ResourceLocation key, Function<CompoundTag, FaultyCardMode> supplier) {
        REGISTRY.put(key, supplier);
    }

    public static void register(String namespace, String path, Function<CompoundTag, FaultyCardMode> supplier) {
        register(new ResourceLocation(namespace, path), supplier);
    }

    // I don't like this, but it works
    public abstract String getType();
    public abstract InteractionResult onItemUse(ItemStack stack, UseOnContext context);

    public CompoundTag save(CompoundTag tag) {
        tag.putString(MODE_TYPE, getType());
        return tag;
    }

    static {
        register(MAE2.toKey(new AoEPaste().getType()), AoEPaste::new);
    }
}

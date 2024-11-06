package stone.mae2.item.faulty;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import stone.mae2.MAE2;

public abstract class FaultyCardMode {
    private static final Map<ResourceLocation, Supplier<FaultyCardMode>> REGISTRY = new HashMap<>();
    private static final NavigableSet<ResourceLocation> CYCLE_ORDER = new TreeSet<>();
    private static final String MODE_TYPE = "type";
    private static final String FAULTY_DATA = "faulty_data";

    public static FaultyCardMode of(ItemStack stack) {
        CompoundTag data = getData(stack);
        var supplier = REGISTRY.get(new ResourceLocation(data.getString(MODE_TYPE)));
        if (supplier == null) {
            return new AoEPaste();
        } else {
            return supplier.get().load(data);
        }
    }

    public static ResourceLocation getResourceLocation(CompoundTag tag) {
        return new ResourceLocation(tag.getString(MODE_TYPE));
    }

    public static CompoundTag getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.getCompound(FAULTY_DATA);
    }

    public static void register(ResourceLocation key, Supplier<FaultyCardMode> supplier) {
        REGISTRY.put(key, supplier);
        CYCLE_ORDER.add(key);
    }

    public static void register(String namespace, String path, Supplier<FaultyCardMode> supplier) {
        register(new ResourceLocation(namespace, path), supplier);
    }

    // I don't like this, but it works. Really should be static somehow
    public abstract ResourceLocation getType();

    public abstract InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context);

    public InteractionResultHolder<ItemStack> onItemUse(Level level, Player player, InteractionHand hand) {
        // do nothing by default, some modes won't need this at all
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * Save this mode's information to NBT to be loaded later
     * 
     * Overriders should call this super method and use the returned tag to put in
     * data. Overriders should then return that same tag so subclasses can use it.
     * 
     * @param tag
     * @return
     */
    public CompoundTag save(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        data.putString(MODE_TYPE, getType().toString());
        tag.put(FAULTY_DATA, data);
        return data;
    }

    protected FaultyCardMode load(CompoundTag tag) {
        return this;
    }

    static {
        register(new AoEPaste().getType(), AoEPaste::new);
        register(new GlobalPaste().getType(), GlobalPaste::new);
    }

    protected abstract Component getName();
    public abstract int getTintColor();

    public static FaultyCardMode cycleMode(FaultyCardMode mode, boolean cycleForward) {
        ResourceLocation current = mode.getType();
        ResourceLocation next = cycleForward ? CYCLE_ORDER.higher(current) : CYCLE_ORDER.lower(current);
        if (next == null) {
            next = cycleForward ? CYCLE_ORDER.first() : CYCLE_ORDER.last();
        }
        return REGISTRY.get(next).get();
    }
}

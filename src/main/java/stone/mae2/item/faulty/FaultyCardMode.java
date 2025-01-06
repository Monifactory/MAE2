/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see https://www.gnu.org/licenses/.
 */
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

    /**
     * Extract out the {@link CompoundTag} for this {@link FaultyMemoryCardItem}'s data
     */ 
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

    /**
     * Called from the onItemUseFirst method in the {@link FaultyMemoryCardItem}
     *
     * Called before the item's actually used on the part/block so it can be
     * cancelled or modified to change what the default {@link
     * appeng.items.tools.MemoryCardItem} behavior is.
     */
    public abstract InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context);

    /**
     * Called from the onItemUse method in the {@link FaultyMemoryCardItem}
     *
     * Mainly for modes to implement "submodes" that players can cycle between
     * with right clicks
     */
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
        // List of Colors in use

        // Black > White to show the size of AoE + Grays might be hard to discern
        register(new AoEPaste().getType(), AoEPaste::new);
        // Orange
        register(new GlobalPaste().getType(), GlobalPaste::new);
        // Magenta
        register(new Incrementing().getType(), Incrementing::new);
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

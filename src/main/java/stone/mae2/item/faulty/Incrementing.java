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

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.parts.storagebus.StorageBusPart;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import stone.mae2.MAE2;
import stone.mae2.util.TransHelper;

public class Incrementing extends FaultyCardMode {
    private static final String IS_INCREMENTING = "incrementing";
    
    private boolean isIncrementing;

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof IPartHost partHost) {
            SelectedPart selectedPart = partHost.selectPartWorld(context.getClickLocation());
            if (selectedPart.part != null && selectedPart.side != null && stack.getItem() instanceof IMemoryCard card) {
                CompoundTag data = card.getData(stack);
                if (AbstractLevelEmitterPart.class.isAssignableFrom(selectedPart.part.getClass())) {
                    // yes the tag name is hardcoded in AE2 too
                    data.putLong("reportingValue", data.getLong("reportingValue") + (isIncrementing ? 1 : -1));
                }
                if (StorageBusPart.class.isAssignableFrom(selectedPart.part.getClass())) {
                    // yes the tag name is hardcoded in AE2 too
                    data.putInt("priority", data.getInt("priority") + (isIncrementing ? 1 : -1));
                }

                stack.getOrCreateTag().put("Data", data);
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> onItemUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        this.isIncrementing = !isIncrementing;
        this.save(stack.getOrCreateTag());
        player.displayClientMessage(getName(), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    protected FaultyCardMode load(CompoundTag tag) {
        this.isIncrementing = tag.getBoolean(IS_INCREMENTING);
        return this;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag data = super.save(tag);
        data.putBoolean(IS_INCREMENTING, this.isIncrementing);
        return data;
    }

    @Override
    public ResourceLocation getType() {
        return MAE2.toKey("incrementing");
    }
    
    @Override
    protected Component getName() {
        return Component.translatable(TransHelper.GUI.toKey("faulty", isIncrementing ? "increment" : "decrement"));
    }

    @Override
    public int getTintColor() {
        int p = AEColor.MAGENTA.mediumVariant;
        if (isIncrementing) {
            int saturate = ((p & 0x808080) >> 7) * 255;
            p = ((p & 0x7F7F7F) << 1) | saturate;
        }
        return p;
    }
}

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

import java.util.List;

import appeng.core.localization.Tooltips;
import appeng.helpers.IMouseWheelItem;
import appeng.items.tools.MemoryCardItem;
import appeng.util.InteractionUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class FaultyMemoryCardItem extends MemoryCardItem implements IMouseWheelItem {
    public FaultyMemoryCardItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
    	// copying/special tool use
        if (context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (!level.isClientSide()) {
            return FaultyCardMode.of(stack).onItemUseFirst(stack, context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    	ItemStack handStack = player.getItemInHand(hand);
        if (InteractionUtil.isInAlternateUseMode(player)) {
            this.cycleMode(player, handStack, true);
            return InteractionResultHolder.consume(handStack);
        } else {
            return FaultyCardMode.of(handStack).onItemUse(level, player, hand);
        }
    }

    private void cycleMode(Player player, ItemStack cardStack, boolean cycleForward) {
        FaultyCardMode nextMode = FaultyCardMode.cycleMode(FaultyCardMode.of(cardStack), cycleForward);
        nextMode.save(cardStack.getOrCreateTag());
        if (player != null) {
            player.displayClientMessage(nextMode.getName(), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
        lines.add(Tooltips.of(FaultyCardMode.of(stack).getName()));
        super.appendHoverText(stack, level, lines, advancedTooltips);
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement(TAG_DISPLAY);
        if (compoundTag != null && compoundTag.contains(TAG_COLOR, 99)) {
            return compoundTag.getInt(TAG_COLOR);
        }
        return 0xFF0000;
    }
    
    public static int getTintColor(ItemStack stack, int index) {
        if (stack.getItem() instanceof FaultyMemoryCardItem card) {
            switch (index) {
            case 1:
                return card.getColor(stack);
            case 2:
                return FaultyCardMode.of(stack).getTintColor();
            default:
                return 0xFFFFFF;
            }
        } else {
            return 0xFFFFFF;
        }
    }

    @Override
    public void onWheel(ItemStack is, boolean up) {
        this.cycleMode(null, is, up);
    }
}

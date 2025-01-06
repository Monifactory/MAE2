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
package stone.mae2.block.crafting;

import appeng.block.crafting.CraftingBlockItem;
import appeng.core.AEConfig;
import appeng.util.InteractionUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class DynamicCraftingBlockItem extends CraftingBlockItem {

    private Supplier<ItemLike> disassemblyCasing;

    public DynamicCraftingBlockItem(Block id, Properties props,
        Supplier<ItemLike> disassemblyCasing,
        Supplier<ItemLike> disassemblyExtra) {
        super(id, props, disassemblyExtra);
        this.disassemblyCasing = disassemblyCasing;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
        InteractionHand hand) {
        if (AEConfig.instance().isDisassemblyCraftingEnabled()
            && InteractionUtil.isInAlternateUseMode(player))
        {
            int itemCount = player.getItemInHand(hand).getCount();
            player.setItemInHand(hand, ItemStack.EMPTY);

            player.getInventory()
                .placeItemBackInInventory(new ItemStack(disassemblyCasing.get(), itemCount));
            player.getInventory()
                .placeItemBackInInventory(new ItemStack(disassemblyExtra.get(), itemCount));

            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),
                level.isClientSide());
        }
        return super.use(level, player, hand);
    }

}

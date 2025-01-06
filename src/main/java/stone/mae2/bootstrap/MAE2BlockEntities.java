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
package stone.mae2.bootstrap;

import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.crafting.CraftingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

public abstract class MAE2BlockEntities {

    public static BlockEntityType<CraftingBlockEntity> DENSE_ACCELERATOR;

    public static void init(IEventBus bus) {
        bus.addListener((RegisterEvent event) ->
        {
            event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper ->
            {
                DENSE_ACCELERATOR = BlockEntityType.Builder
                    .of((BlockPos pos,
                        BlockState state) -> new CraftingBlockEntity(DENSE_ACCELERATOR, pos, state),
                        MAE2Blocks.ACCELERATOR_4x.get(), MAE2Blocks.ACCELERATOR_16x.get(),
                        MAE2Blocks.ACCELERATOR_64x.get(), MAE2Blocks.ACCELERATOR_256x.get())
                    .build(null);
                helper.register("dense_accelerator", DENSE_ACCELERATOR);

                BlockEntityTicker<CraftingBlockEntity> serverTicker = null;
                if (ServerTickingBlockEntity.class.isAssignableFrom(CraftingBlockEntity.class))
                {
                    serverTicker = (level, pos, state, entity) ->
                    {
                        ((ServerTickingBlockEntity) entity).serverTick();
                    };
                }
                BlockEntityTicker<CraftingBlockEntity> clientTicker = null;
                if (ClientTickingBlockEntity.class.isAssignableFrom(CraftingBlockEntity.class))
                {
                    clientTicker = (level, pos, state, entity) ->
                    {
                        ((ClientTickingBlockEntity) entity).clientTick();
                    };
                }
                MAE2Blocks.ACCELERATOR_4x.get().setBlockEntity(CraftingBlockEntity.class,
                    DENSE_ACCELERATOR, clientTicker, serverTicker);
                MAE2Blocks.ACCELERATOR_16x.get().setBlockEntity(CraftingBlockEntity.class,
                    DENSE_ACCELERATOR, clientTicker, serverTicker);
                MAE2Blocks.ACCELERATOR_64x.get().setBlockEntity(CraftingBlockEntity.class,
                    DENSE_ACCELERATOR, clientTicker, serverTicker);
                MAE2Blocks.ACCELERATOR_256x.get().setBlockEntity(CraftingBlockEntity.class,
                    DENSE_ACCELERATOR, clientTicker, serverTicker);
            });
        });
    }
}

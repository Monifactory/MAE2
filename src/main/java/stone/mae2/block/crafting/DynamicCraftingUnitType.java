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

import appeng.block.crafting.ICraftingUnitType;
import net.minecraft.world.item.Item;

import stone.mae2.bootstrap.MAE2Items;

public enum DynamicCraftingUnitType implements ICraftingUnitType {
    ACCELERATOR_4x(0, 4), ACCELERATOR_16x(0, 16), ACCELERATOR_64x(0, 64), ACCELERATOR_256x(0, 256);

    private final long storage;
    private final int threads;

    DynamicCraftingUnitType(long storage, int threads) {
        this.storage = storage;
        this.threads = threads;
    }

    @Override
    public long getStorageBytes() {
        return this.storage;
    }

    @Override
    public int getAcceleratorThreads() {
        return this.threads;
    }

    @Override
    public Item getItemFromType() {
        var item = switch (this) {
        case ACCELERATOR_4x -> MAE2Items.ACCELERATOR_4x;
        case ACCELERATOR_16x -> MAE2Items.ACCELERATOR_16x;
        case ACCELERATOR_64x -> MAE2Items.ACCELERATOR_64x;
        case ACCELERATOR_256x -> MAE2Items.ACCELERATOR_256x;
        };
        return item.get();
    }

}

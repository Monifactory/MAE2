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
package stone.mae2.parts.p2p;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;

public interface PatternP2PTunnel {
    @Nonnull
    public List<TunneledPatternProviderTarget> getTargets();

    @Nonnull
    public List<TunneledPos> getTunneledPositions();
    

    public record TunneledPos(BlockPos pos, Direction dir) {

        private static final String POSITION = "mae2Pos";
        private static final String DIRECTION = "mae2Direction";

        public void writeToNBT(CompoundTag tag) {
            tag.putLong(POSITION, pos.asLong());
            tag.putByte(DIRECTION, (byte) dir.get3DDataValue());
        }

        public static TunneledPos readFromNBT(CompoundTag tag) {
            return new TunneledPos(BlockPos.of(tag.getLong(POSITION)),
                    Direction.from3DDataValue(tag.getByte(DIRECTION)));
        }
    }

    /**
     * A holder for a pattern provider target through a pattern P2P
     * 
     * if target is null it means this is actually for the default blocks around the
     * provider (ie its not tunneled and should act like normal)
     */
    public record TunneledPatternProviderTarget(PatternProviderTargetCache target,
            TunneledPos pos) {
    }
}

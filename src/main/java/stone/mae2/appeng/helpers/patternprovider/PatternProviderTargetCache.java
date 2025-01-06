/*
 * This file is part of AE2.
 *
 * AE2 is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * AE2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.appeng.helpers.patternprovider;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.storage.CompositeStorage;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.BlockApiCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Has to be like this because it's not public in ae2, and I can't figure out a
 * way to access transform it
 * 
 * I claim no ownership of this code, it's a direct copy/paste from AE2.
 */
public class PatternProviderTargetCache {
    private final BlockApiCache<MEStorage> cache;
    private final Direction direction;
    private final IActionSource src;
    private final Map<AEKeyType, ExternalStorageStrategy> strategies;

    public PatternProviderTargetCache(ServerLevel l, BlockPos pos,
        Direction direction,
        IActionSource src) {
        this.cache = BlockApiCache.create(Capabilities.STORAGE, l, pos);
        this.direction = direction;
        this.src = src;
        this.strategies = StackWorldBehaviors.createExternalStorageStrategies(l,
            pos, direction);
    }

    @Nullable
    public
    PatternProviderTarget find() {
        // our capability first: allows any storage channel
        var meStorage = cache.find(direction);
        if (meStorage != null)
        {
            return wrapMeStorage(meStorage);
        }

        // otherwise fall back to the platform capability
        var externalStorages = new IdentityHashMap<AEKeyType, MEStorage>(2);
        for (var entry : strategies.entrySet())
        {
            var wrapper = entry.getValue().createWrapper(false, () ->
            {
            });
            if (wrapper != null)
            {
                externalStorages.put(entry.getKey(), wrapper);
            }
        }

        if (externalStorages.size() > 0)
        {
            return wrapMeStorage(new CompositeStorage(externalStorages));
        }

        return null;
    }

    private PatternProviderTarget wrapMeStorage(MEStorage storage) {
        return new PatternProviderTarget()
        {
            @Override
            public long insert(AEKey what, long amount, Actionable type) {
                return storage.insert(what, amount, type, src);
            }

            @Override
            public boolean containsPatternInput(Set<AEKey> patternInputs) {
                for (var stack : storage.getAvailableStacks())
                {
                    if (patternInputs.contains(stack.getKey().dropSecondary()))
                    {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}

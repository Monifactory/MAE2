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
package stone.mae2.parts.p2p.multi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import stone.mae2.MAE2;
import stone.mae2.me.service.MultiP2PService;
import appeng.api.parts.IPartItem;
import appeng.hooks.ticking.TickHandler;

/**
 * Base class for simple capability-based p2p tunnels. Don't forget to set the 3 handlers in the constructor of the
 * child class!
 */
public abstract class CapabilityMultiP2PPart<P extends CapabilityMultiP2PPart<P, A>, A> extends MultiP2PTunnelPart<P> {
        private final Capability<A> capability;
    // Prevents recursive block updates.
    private boolean inBlockUpdate = false;
    // Prevents recursive access to the adjacent capability in case P2P input/output faces touch
    private int accessDepth = 0;
    private final CapabilityGuard capabilityGuard = new CapabilityGuard();
    private final EmptyCapabilityGuard emptyCapabilityGuard = new EmptyCapabilityGuard();
    protected A inputHandler;
    protected A outputHandler;
    protected A emptyHandler;

    public CapabilityMultiP2PPart(IPartItem<?> partItem, Capability<A> capability) {
        super(partItem);
        this.capability = capability;
    }

    @Override
    protected float getPowerDrainPerTick() {
        return 2.0f;
    }

    public final <T> LazyOptional<T> getCapability(Capability<T> capabilityClass) {
        if (capabilityClass == capability) {
            if (isOutput()) {
                return LazyOptional.of(() -> outputHandler).cast();
            } else {
                return LazyOptional.of(() -> inputHandler).cast();
            }
        }
        return LazyOptional.empty();
    }

    /**
     * Return the capability connected to this side of this P2P connection. If this method is called again on this
     * tunnel while the returned object has not been closed, further calls to {@link CapabilityGuard#get()} will return
     * a dummy capability.
     */
    protected final CapabilityGuard getAdjacentCapability() {
        accessDepth++;
        return capabilityGuard;
    }

    protected class CapabilityGuard implements AutoCloseable {
        /**
         * Get the capability, or a null handler if not available. Use within the scope of the enclosing AdjCapability.
         */
        public A get() {
            if (accessDepth == 0) {
                throw new IllegalStateException("get was called after closing the wrapper");
            } else if (accessDepth == 1) {
                if (isActive()) {
                    var self = getBlockEntity();
                    var te = self.getLevel().getBlockEntity(getFacingPos());

                    if (te != null) {
                        return te.getCapability(capability, getSide().getOpposite())
                                .orElse(emptyHandler);
                    }
                }

                return emptyHandler;
            } else {
                // This capability is already in use (as the nesting is > 1), so we return an empty handler to prevent
                // infinite recursion.
                return emptyHandler;
            }
        }

        @Override
        public void close() {
            if (--accessDepth < 0) {
                throw new IllegalStateException("Close has been called multiple times");
            }
        }
    }

    /**
     * The position right in front of this P2P tunnel.
     */
    private BlockPos getFacingPos() {
        return getHost().getLocation().getPos().relative(getSide());
    }

    /**
     * This specialization is used when the tunnel is not connected.
     */
    protected class EmptyCapabilityGuard extends CapabilityGuard implements AutoCloseable {
        @Override
        public void close() {
        }

        @Override
        public A get() {
            return emptyHandler;
        }
    }

    // Send a block update on p2p status change, or any update on another endpoint.
    protected void sendBlockUpdate() {
        // Prevent recursive block updates.
        if (!inBlockUpdate) {
            inBlockUpdate = true;

            try {
                // getHost().notifyNeighbors() would queue a callback, but we want to do an update synchronously!
                // (otherwise we can't detect infinite recursion, it would just queue updates endlessly)
                getHost().notifyNeighborNow(getSide());
            } finally {
                inBlockUpdate = false;
            }
        }
    }

    @Override
    public void onTunnelNetworkChange() {
        // This might be invoked while the network is being unloaded and we don't want to send a block update then, so
        // we delay it until the next tick.
        TickHandler.instance().addCallable(getLevel(), () -> {
            if (getMainNode().isReady()) { // Check that the p2p tunnel is still there.
                sendBlockUpdate();
            }
        });
    }

    /**
     * Forward block updates from the attached tile's position to the other end of the tunnel. Required for TE's on the
     * other end to know that the available caps may have changed.
     */
    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        // We only care about block updates on the side this tunnel is facing
        if (!getFacingPos().equals(neighbor)) {
            return;
        }

        // Prevent recursive block updates.
        if (!inBlockUpdate) {
            inBlockUpdate = true;

            try {
                if (isOutput()) {
                    getInputStream().forEach(input -> input.sendBlockUpdate());
                    
                } else {
                    getOutputStream().forEach(output -> output.sendBlockUpdate());
                }
            } finally {
                inBlockUpdate = false;
            }
        }
    }
}

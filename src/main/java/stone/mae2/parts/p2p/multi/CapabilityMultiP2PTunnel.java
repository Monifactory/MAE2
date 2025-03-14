/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.parts.p2p.multi;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.hooks.ticking.TickHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Base class for simple capability-based p2p tunnels. Don't forget to set the 3
 * handlers in the constructor of the child class!
 */
public abstract class CapabilityMultiP2PTunnel<T extends CapabilityMultiP2PTunnel<T, L, P, C>, L extends CapabilityMultiP2PTunnel<T, L, P, C>.Logic, P extends CapabilityMultiP2PTunnel.Part<T, L, P, C>, C> extends MultiP2PTunnel<T, L, P> {
  protected abstract Capability<C> getCapability();

  protected C inputHandler;
  protected C outputHandler;
  protected C emptyHandler;

  public CapabilityMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
  }

  public class Logic extends MultiP2PTunnel<T, L, P>.Logic {
    private final CapabilityGuard capabilityGuard = new CapabilityGuard();

    public Logic(P part) {
      super(part);
    }

    public final <A> LazyOptional<A> getCapability(
      Capability<A> capabilityClass) {
      if (capabilityClass == CapabilityMultiP2PTunnel.this.getCapability()) {
        if (this.part.isOutput()) {
          return LazyOptional.of(() -> outputHandler).cast();
        } else {
          return LazyOptional.of(() -> inputHandler).cast();
        }
      }
      return LazyOptional.empty();

    }

    /**
     * Return the capability connected to this side of this P2P connection. If
     * this method is called again on this tunnel while the returned object has
     * not been closed, further calls to {@link CapabilityGuard#get()} will
     * return a dummy capability.
     */
    private int accessDepth = 0;

    protected final CapabilityGuard getAdjacentCapability() {
      accessDepth++;
      return capabilityGuard;
    }

    protected class CapabilityGuard implements AutoCloseable {
      /**
       * Get the capability, or a null handler if not available. Use within the
       * scope of the enclosing AdjCapability.
       */
      public C get() {
        if (accessDepth == 0) {
          throw new IllegalStateException(
            "get was called after closing the wrapper");
        } else if (accessDepth == 1) {
          if (Logic.this.part.isActive()) {
            var self = Logic.this.part.getBlockEntity();
            var te = self.getLevel().getBlockEntity(getFacingPos());

            if (te != null) {
              return te
                .getCapability(CapabilityMultiP2PTunnel.this.getCapability(),
                  Logic.this.part.getSide().getOpposite())
                .orElse(emptyHandler);
            }
          }

          return emptyHandler;
        } else {
          // This capability is already in use (as the nesting is > 1), so we
          // return an
          // empty handler to prevent
          // infinite recursion.
          return emptyHandler;
        }
      }

      @Override
      public void close() {
        if (--accessDepth < 0) {
          throw new IllegalStateException(
            "Close has been called multiple times");
        }
      }
    }

    /**
     * The position right in front of this P2P tunnel.
     */
    private BlockPos getFacingPos() {
      return this.part.getHost().getLocation().getPos()
        .relative(this.part.getSide());
    }

    // Send a block update on p2p status change, or any update on another
    // endpoint.
    private boolean inBlockUpdate = false;

    protected void sendBlockUpdate() {
      // Prevent recursive block updates.
      if (!inBlockUpdate) {
        inBlockUpdate = true;

        try {
          // getHost().notifyNeighbors() would queue a callback, but we want to
          // do an
          // update synchronously!
          // (otherwise we can't detect infinite recursion, it would just queue
          // updates
          // endlessly)
          this.part.getHost().notifyNeighborNow(this.part.getSide());
        } finally {
          inBlockUpdate = false;
        }
      }
    }

    @Override
    public void onTunnelNetworkChange() {
      // This might be invoked while the network is being unloaded and we don't
      // want
      // to send a block update then, so
      // we delay it until the next tick.
      TickHandler.instance().addCallable(this.part.getLevel(), () -> {
        if (this.part.getMainNode().isReady()) { // Check that the p2p tunnel is
                                                 // still there.
          sendBlockUpdate();
        }
      });
    }

    /**
     * Forward block updates from the attached tile's position to the other end
     * of the tunnel. Required for TE's on the other end to know that the
     * available caps may have changed.
     */
    public void onNeighborChanged(BlockGetter level, BlockPos pos,
      BlockPos neighbor) {
      // We only care about block updates on the side this tunnel is facing
      if (!getFacingPos().equals(neighbor)) {
        return;
      }

      // Prevent recursive block updates.
      if (!inBlockUpdate) {
        inBlockUpdate = true;

        try {
          if (this.part.isOutput()) {
            for (var output : CapabilityMultiP2PTunnel.this.getOutputs()) {
              output.sendBlockUpdate();
            }
          } else {
            for (var input : CapabilityMultiP2PTunnel.this.getInputs()) {
              input.sendBlockUpdate();
            }
          }
        } finally {
          inBlockUpdate = false;
        }
      }
    }
  }

  public abstract static class Part<T extends CapabilityMultiP2PTunnel<T, L, P, C>, L extends CapabilityMultiP2PTunnel<T, L, P, C>.Logic, P extends Part<T, L, P, C>, C> extends MultiP2PTunnel.Part<T, L, P> {
    public Part(IPartItem<?> partItem) {
      super(partItem);
    }

    @Override
    protected float getPowerDrainPerTick() {
      return 2.0f;
    }

    public final <A> LazyOptional<A> getCapability(
      Capability<A> capabilityClass) {
      if (this.logic != null) {
        return this.logic.getCapability(capabilityClass);
      } else {
        return LazyOptional.empty();
      }
    }

    public void onNeighborChanged(BlockGetter level, BlockPos pos,
      BlockPos neighbor) {
      if (this.logic != null)
        this.logic.onNeighborChanged(level, pos, neighbor);
    }
  }
}

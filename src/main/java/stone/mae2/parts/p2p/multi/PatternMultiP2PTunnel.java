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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.p2p.P2PModels;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import stone.mae2.MAE2;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;
import stone.mae2.parts.p2p.PatternP2PTunnel;
import stone.mae2.parts.p2p.PatternP2PTunnel.TunneledPatternProviderTarget;
import stone.mae2.parts.p2p.PatternP2PTunnel.TunneledPos;

public class PatternMultiP2PTunnel extends MultiP2PTunnel<PatternMultiP2PTunnel, PatternMultiP2PTunnel.Logic, PatternMultiP2PTunnel.Part> {
  public PatternMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
  }

  public class Logic extends MultiP2PTunnel<PatternMultiP2PTunnel, Logic, Part>.Logic {
    public Logic(Part part) {
      super(part);
    }

    public List<TunneledPatternProviderTarget> getTargets() {
      List<TunneledPatternProviderTarget> targets = new ArrayList<>();
      for (Logic output : outputs) {
        Part part = output.part;
        TunneledPatternProviderTarget target = new TunneledPatternProviderTarget(
          part.getTarget(),
          new TunneledPos(
            part.getBlockEntity().getBlockPos().relative(part.getSide()),
            part.getSide()));
        if (target.target() != null) {
          targets.add(target);
        }
      }
      return targets;
    }

    public TunneledPos getInputPos() {
      Part input;
      try {
        input = inputs.iterator().next().part;
      } catch (NoSuchElementException e) {
        return null;
      }
      Direction inputSide = input.getSide();
      return new TunneledPos(
        input.getBlockEntity().getBlockPos().relative(inputSide),
        inputSide.getOpposite());
    }

    public List<TunneledPos> getTunneledPositions() {
      List<TunneledPos> tunneled = new ArrayList<>();
      for (Logic output : outputs) {
        Part part = output.part;
        Direction outputSide = part.getSide();
        tunneled.add(new TunneledPos(
          part.getBlockEntity().getBlockPos().relative(outputSide),
          outputSide.getOpposite()));
      }
      return tunneled;
    }

  }

  public static class Part extends MultiP2PTunnel.Part<PatternMultiP2PTunnel, Logic, Part> implements PatternP2PTunnel {
    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_pattern"));

    private PatternProviderTargetCache cache;

    @PartModels
    public static List<IPartModel> getModels() {
      return MODELS.getModels();
    }

    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    public Part(IPartItem<?> partItem) {
      super(partItem);
    }

    @Override
    public void addToWorld() {
      super.addToWorld();
      Level level = this.getBlockEntity().getLevel();
      if (!level.isClientSide) {
        this.cache = new PatternProviderTargetCache((ServerLevel) level,
          this.getBlockEntity().getBlockPos().relative(this.getSide()),
          this.getSide().getOpposite(),
          new MachineSource(this.getMainNode()::getNode));
      }
    }

    @Nonnull
    @Override
    public List<TunneledPatternProviderTarget> getTargets() {
      if (this.isOutput())
      // you can't go through a output tunnel (duh)
      {
        return List.of();
      } else {
        return this.logic.getTargets();
      }
    }

    @Nullable
    private PatternProviderTargetCache getTarget() {
      return cache;
    }

    @Nullable
    @Override
    public List<TunneledPos> getTunneledPositions() {
      if (this.isOutput()) {
        return List.of();
      } else {
        return this.logic.getTunneledPositions();
      }
    }

    // TODO make the outputs link to every input, for the rare case 1 provider
    // can't
    // keep up with it
    private TunneledPos getInputPos() {
      return this.logic.getInputPos();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability) {
      if (this.isOutput()) {
        TunneledPos provider = this.getInputPos();
        if (provider == null)
          return LazyOptional.empty();
        BlockEntity providerEntity = this.getLevel()
          .getBlockEntity(provider.pos());
        if (providerEntity != null) {
          return providerEntity.getCapability(capability, provider.dir());
        } else {
          return LazyOptional.empty();
        }
      } else {
        return LazyOptional.empty();
      }
    }

    @Override
    public PatternMultiP2PTunnel createTunnel(short freq) {
      return new PatternMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<PatternMultiP2PTunnel> getTunnelClass() {
      return PatternMultiP2PTunnel.class;
    }
  }

  @Override
  public Logic createLogic(Part part) {
    return part.setLogic(new Logic(part));
  }
}

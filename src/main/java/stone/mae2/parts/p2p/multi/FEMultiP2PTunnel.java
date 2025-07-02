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

import appeng.api.config.PowerUnits;
import appeng.api.networking.IGrid;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import stone.mae2.MAE2;
import stone.mae2.api.Tickable;

import java.util.List;

public class FEMultiP2PTunnel extends
  CapabilityMultiP2PTunnel<FEMultiP2PTunnel, FEMultiP2PTunnel.Logic, FEMultiP2PTunnel.Part, IEnergyStorage>
  implements Tickable {
  private static final IEnergyStorage NULL_ENERGY_STORAGE = new NullHandler();

  /**
   * Buffer of energy used to separate inputs from outputs
   * 
   * P2P inputs dump into this which then gets distributed out to each P2P
   * output. This is intended to reduce the computational complexity of the P2P.
   * Otherwise if each input maps to each output, the number of operations goes
   * quadratic
   */
  // Persisted
  private long buffer;

  /**
   * How much FE has been extracted in between distributions
   * 
   * This is needed because it's possible to extract energy from the buffer via
   * the outputs of the tunnel. This'd bypass the normal distribution logic, and
   * thus needs to be accounted for.
   */
  private long extracted;

  /**
   * Flag for if we have enough FE in the buffer for another distribution
   * 
   * Set every distribution if there's more left in buffer then was distributed.
   * P2P input's won't accept FE while this is set.
   */
  private boolean isSatisfied;

  public FEMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
    this.inputHandler = new InputHandler();
    this.outputHandler = new OutputHandler();
    this.emptyHandler = NULL_ENERGY_STORAGE;
  }

  public static final String ENERGY_TAG = "energy";

  @Override
  public CompoundTag saveNodeData(Part part) {
    CompoundTag data = super.saveNodeData(part);
    if (data == null)
      data = new CompoundTag();
    // grid seems to remove part before calling this, hence +1
    long split = this.buffer / (this.inputs.size() + 1);
    this.buffer -= split;
    data.putLong(ENERGY_TAG, split);
    return data;
  }

  @Override
  public Logic addTunnel(Part part, Tag tag) {
    if (tag instanceof CompoundTag data)
      this.buffer += data.getLong(ENERGY_TAG);
    return addTunnel(part);
  }

  public static class Part extends
    CapabilityMultiP2PTunnel.Part<FEMultiP2PTunnel, Logic, Part, IEnergyStorage> {
    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_fe"));

    public Part(IPartItem<?> partItem) {
      super(partItem);
    }

    @PartModels
    public static List<IPartModel> getModels() { return MODELS.getModels(); }

    @Override
    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public FEMultiP2PTunnel createTunnel(short freq) {
      return new FEMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<FEMultiP2PTunnel> getTunnelClass() {
      return FEMultiP2PTunnel.class;
    }
  }

  public class Logic extends
    CapabilityMultiP2PTunnel<FEMultiP2PTunnel, Logic, Part, IEnergyStorage>.Logic {

    public Logic(Part part) {
      super(part);
    }
  }

  private class InputHandler implements IEnergyStorage {
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
      if (isSatisfied) {
        return 0;
      } else {
        if (!simulate)
          // yes this breaks if more than max long is transfered, too bad!
          buffer += maxReceive;
        return maxReceive;
      }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
      return 0;
    }

    // since this can store past max int, it's nonsensical to translate back to
    // FE's API. Instead just look like it's empty and let others push as much
    // as they want in. Worst case it'll get capped via the satisfaction flag
    @Override
    public int getEnergyStored() { return 0; }

    @Override
    public int getMaxEnergyStored() { return Integer.MAX_VALUE; }

    @Override
    public boolean canExtract() {
      return false;
    }

    @Override
    public boolean canReceive() {
      return true;
    }
  }

  private class OutputHandler implements IEnergyStorage {
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
      return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
      long energyExtracted = Math.min(buffer, maxExtract);
      if (!simulate) {
        buffer -= energyExtracted;
        // tax'll come during distribution
        extracted += energyExtracted;
      }
      // cast is okay because energyExtracted will never be above an int
      return (int) energyExtracted;
    }

    @Override
    public int getEnergyStored() {
      try {
        return Math.toIntExact(buffer);
      } catch (ArithmeticException e) {
        return Integer.MAX_VALUE;
      }
    }

    @Override
    public int getMaxEnergyStored() { return Integer.MAX_VALUE; }

    @Override
    public boolean canExtract() {
      return true;
    }

    @Override
    public boolean canReceive() {
      return false;
    }
  }

  private static class NullHandler implements IEnergyStorage {
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
      return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
      return 0;
    }

    @Override
    public int getEnergyStored() { return 0; }

    @Override
    public int getMaxEnergyStored() { return 0; }

    @Override
    public boolean canExtract() {
      return false;
    }

    @Override
    public boolean canReceive() {
      return false;
    }
  }

  @Override
  protected Capability<IEnergyStorage> getCapability() {
    return ForgeCapabilities.ENERGY;
  }

  @Override
  public Logic createLogic(Part part) {
    return part.setLogic(new Logic(part));
  }

  @Override
  public TickingRequest getTickingRequest() {
    return Tickable
      .toTickingRequest(MAE2.CONFIG.parts().rates().FEMultiP2PTunnel(), false,
        false);
  }

  @Override
  public TickRateModulation tick() {
    boolean didWork = false;
    long distributed = this.extracted;
    this.extracted = 0;
    for (var output : this.outputs) {
      try (var guard = output.getAdjacentCapability()) {
        int toInsert;
        try {
          toInsert = Math.toIntExact(this.buffer);
        } catch (ArithmeticException e) {
          toInsert = Integer.MAX_VALUE;
        }
        int inserted = guard.get().receiveEnergy(toInsert, false);
        didWork |= inserted > 0;
        distributed += inserted;
        this.buffer -= inserted;
      }
    }
    this.isSatisfied = this.buffer / 2 > distributed;
    this.deductEnergyCost(distributed, PowerUnits.FE);
    return didWork ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
  }
}

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
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;

import stone.mae2.MAE2;
import stone.mae2.api.Tickable;

import java.util.List;

public class EUMultiP2PTunnel extends
  CapabilityMultiP2PTunnel<EUMultiP2PTunnel, EUMultiP2PTunnel.Logic, EUMultiP2PTunnel.Part, IEnergyContainer>
  implements Tickable {
  private static final IEnergyContainer NULL_ENERGY_STORAGE = new NullHandler();

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
   * How much EU has been extracted in between distributions
   * 
   * This is needed because it's possible to extract energy from the buffer via
   * the outputs of the tunnel. This'd bypass the normal distribution logic, and
   * thus needs to be accounted for.
   */
  private long extracted;
  // prevents divide by zero errors
  private long maxVoltage = 1;

  /**
   * Flag for if we have enough EU in the buffer for another distribution
   * 
   * Set every distribution if there's more left in buffer then was distributed.
   * P2P input's won't accept EU while this is set.
   */
  private boolean isSatisfied;

  public EUMultiP2PTunnel(short freq, IGrid grid) {
    super(freq, grid);
    this.inputHandler = new InputHandler();
    this.outputHandler = new OutputHandler();
    this.emptyHandler = NULL_ENERGY_STORAGE;
  }

  @Override
  public Tag saveNodeData(Part part) {
    // grid seems to remove part before calling this, hence +1
    long split = this.buffer / (this.inputs.size() + 1);
    this.buffer -= split;
    return LongTag.valueOf(split);
  }

  @Override
  public Logic addTunnel(Part part, Tag tag) {
    if (tag instanceof LongTag split)
      this.buffer += split.getAsLong();
    return addTunnel(part);
  }

  public static class Part extends
    CapabilityMultiP2PTunnel.Part<EUMultiP2PTunnel, Logic, Part, IEnergyContainer> {
    private static final P2PModels MODELS = new P2PModels(
      MAE2.toKey("part/p2p/multi_p2p_tunnel_eu"));

    public Part(IPartItem<?> partItem) { super(partItem); }

    @PartModels
    public static List<IPartModel> getModels() { return MODELS.getModels(); }

    @Override
    public IPartModel getStaticModels() {
      return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public EUMultiP2PTunnel createTunnel(short freq) {
      return new EUMultiP2PTunnel(freq, this.getGridNode().getGrid());
    }

    @Override
    public Class<EUMultiP2PTunnel> getTunnelClass() {
      return EUMultiP2PTunnel.class;
    }
  }

  public class Logic extends
    CapabilityMultiP2PTunnel<EUMultiP2PTunnel, Logic, Part, IEnergyContainer>.Logic {
    public Logic(Part part) { super(part); }
  }

  private class InputHandler implements IEnergyContainer {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage,
      long amperage) {
      // breaks when voltage and amperage are significant fractions of max int,
      // too bad!
      if (!isSatisfied) {
        buffer += voltage * amperage;
        maxVoltage = Math.max(maxVoltage, voltage);
        return amperage;
      } else {
        return 0;
      }
    }

    @Override
    public boolean inputsEnergy(Direction side) { return true; }

    @Override
    public long changeEnergy(long differenceAmount) { return 0; }

    @Override
    public long getEnergyStored() { return 0; }

    @Override
    public long getEnergyCapacity() { return Long.MAX_VALUE; }

    @Override
    public long getInputAmperage() { return Long.MAX_VALUE; }

    @Override
    public long getInputVoltage() { return Long.MAX_VALUE; }
  }

  private class OutputHandler implements IEnergyContainer {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage,
      long amperage) {
      return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) { return false; }

    @Override
    public long changeEnergy(long differenceAmount) { return 0; }

    @Override
    public long getEnergyStored() { return buffer; }

    @Override
    public long getEnergyCapacity() { return Long.MAX_VALUE; }

    @Override
    public long getInputAmperage() { return 0; }

    @Override
    public long getInputVoltage() { return 0; }
  }

  private static class NullHandler implements IEnergyContainer {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage,
      long amperage) {
      return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) { return false; }

    @Override
    public long changeEnergy(long differenceAmount) { return 0; }

    @Override
    public long getEnergyStored() { return 0; }

    @Override
    public long getEnergyCapacity() { return 0; }

    @Override
    public long getInputAmperage() { return 0; }

    @Override
    public long getInputVoltage() { return 0; }
  }

  @Override
  protected Capability<IEnergyContainer> getCapability() {
    return GTCapability.CAPABILITY_ENERGY_CONTAINER;
  }

  @Override
  public Logic createLogic(Part part) { return part.setLogic(new Logic(part)); }

  @Override
  public TickingRequest getTickingRequest() {
    return Tickable
      .toTickingRequest(MAE2.CONFIG.parts().rates().EUMultiP2PTunnel(), false,
        false);
  }

  @Override
  public TickRateModulation tick() {
    boolean didWork = false;
    long distributed = 0;
    for (var output : this.outputs) {
      try (var guard = output.getAdjacentCapability()) {
        long inserted = guard
          .get()
          .acceptEnergyFromNetwork(output.part.getSide().getOpposite(),
            maxVoltage, buffer / maxVoltage);
        didWork |= inserted > 0;
        distributed += inserted * maxVoltage;
        this.buffer -= inserted * maxVoltage;
      }
    }
    // either stop at double what's being transferred, and one amp (enough to
    // bootstrap the unsatisfaction)
    this.isSatisfied = this.buffer > distributed * 2
      && this.buffer >= maxVoltage;
    this.deductEnergyCost(distributed * FeCompat.ratio(false), PowerUnits.FE);
    return didWork ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
  }
}

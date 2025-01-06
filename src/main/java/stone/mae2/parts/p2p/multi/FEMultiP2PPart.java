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

import java.util.List;

import net.minecraftforge.energy.IEnergyStorage;
import stone.mae2.MAE2;
import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.capabilities.Capabilities;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;

public class FEMultiP2PPart extends CapabilityMultiP2PPart<FEMultiP2PPart, IEnergyStorage> {
    private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/multi_p2p_tunnel_fe"));
    private static final IEnergyStorage NULL_ENERGY_STORAGE = new NullEnergyStorage();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public FEMultiP2PPart(IPartItem<?> partItem) {
        super(partItem, Capabilities.FORGE_ENERGY);
        inputHandler = new InputEnergyStorage();
        outputHandler = new OutputEnergyStorage();
        emptyHandler = NULL_ENERGY_STORAGE;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputEnergyStorage implements IEnergyStorage {
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int total = 0;

            // add counter for output size?
            final int outputTunnels = FEMultiP2PPart.this.getOutputs().size();

            if (outputTunnels == 0 | maxReceive == 0) {
                return 0;
            }

            final int amountPerOutput = maxReceive / outputTunnels;
            int overflow = amountPerOutput == 0 ? maxReceive : maxReceive % amountPerOutput;

            for (FEMultiP2PPart target : FEMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IEnergyStorage output = capabilityGuard.get();
                    final int toSend = amountPerOutput + overflow;
                    final int received = output.receiveEnergy(toSend, simulate);

                    overflow = toSend - received;
                    total += received;
                }
            }

            if (!simulate) {
                FEMultiP2PPart.this.queueTunnelDrain(PowerUnits.FE, total);
            }

            return total;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return FEMultiP2PPart.this.getOutputStream().anyMatch(output -> {
                try (CapabilityGuard guard = output.getAdjacentCapability()) {
                    return guard.get().canReceive();
                }
            });
        }

        @Override
        public int getMaxEnergyStored() {
            int total = 0;

            for (FEMultiP2PPart t : FEMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = t.getAdjacentCapability()) {
                    total += capabilityGuard.get().getMaxEnergyStored();
                }
            }

            return total;
        }

        @Override
        public int getEnergyStored() {
            int total = 0;

            for (FEMultiP2PPart t : FEMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = t.getAdjacentCapability()) {
                    total += capabilityGuard.get().getEnergyStored();
                }
            }

            return total;
        }
    }

    private class OutputEnergyStorage implements IEnergyStorage {
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract == 0) {
                return 0;
            }

            // amount left to extract
            int toExtract = maxExtract;
            
            for (FEMultiP2PPart target : FEMultiP2PPart.this.getInputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IEnergyStorage output = capabilityGuard.get();
                    toExtract -= output.extractEnergy(toExtract, simulate);
                    if (toExtract <= 0)
                        break;
                }
            }

            int extracted = maxExtract - toExtract;
            if (!simulate) {
                FEMultiP2PPart.this.queueTunnelDrain(PowerUnits.FE, extracted);
            }

            return extracted;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return FEMultiP2PPart.this.getInputStream().anyMatch(input -> {
                try (CapabilityGuard guard = input.getAdjacentCapability()) {
                    return guard.get().canExtract();
                }
            });
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public int getMaxEnergyStored() {
            return FEMultiP2PPart.this.getInputStream()
                    .mapToInt(input -> {
                        try (CapabilityGuard guard = input.getAdjacentCapability()) {
                            return guard.get().getMaxEnergyStored();
                        }
                    })
                    .sum();
        }

        @Override
        public int getEnergyStored() {
            return FEMultiP2PPart.this.getInputStream()
                    .mapToInt(input -> {
                        try (CapabilityGuard guard = input.getAdjacentCapability()) {
                            return guard.get().getEnergyStored();
                        }
                    })
                    .sum();
        }
    }

    private static class NullEnergyStorage implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}

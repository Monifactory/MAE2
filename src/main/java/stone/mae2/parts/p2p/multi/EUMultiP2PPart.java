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

import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.Direction;

import stone.mae2.MAE2;
import stone.mae2.me.service.MultiP2PService;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EUMultiP2PPart extends CapabilityMultiP2PPart<EUMultiP2PPart, IEnergyContainer> {
    private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/multi_p2p_tunnel_eu"));
    private static final IEnergyContainer EMPTY_HANDLER = new EmptyHandler();

    public EUMultiP2PPart(IPartItem<?> partItem) {
        super(partItem, GTCapability.CAPABILITY_ENERGY_CONTAINER);
        this.inputHandler = new InputHandler();
        this.outputHandler = new OutputHandler();
        this.emptyHandler = EMPTY_HANDLER;
    }

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    public Set<EUMultiP2PPart> getTickOutputs() {
        if (this.getMainNode().isOnline()) {
            var grid = getMainNode().getGrid();
            if (grid != null) {
                return grid.getService(MultiP2PService.class).getTickOutputs(this.getFrequency(), EUMultiP2PPart.class);
            }
        }
        return Collections.emptySet();
    }

    public class InputHandler implements IEnergyContainer {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            long toSend = amperage;
            long total = 0;
            Iterator<EUMultiP2PPart> it = EUMultiP2PPart.this.getTickOutputs().iterator();
            while (it.hasNext()) {
                EUMultiP2PPart target = it.next();
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IEnergyContainer output = capabilityGuard.get();
                    final long received = output.acceptEnergyFromNetwork(target.getSide().getOpposite(), voltage,
                            toSend);

                    toSend -= received;
                    total += received;
                    if (toSend == 0) {
                        break;
                    }
                }
                it.remove();
            }

            if (total > 0) {
                EUMultiP2PPart.this
                        .queueTunnelDrain(PowerUnits.FE,
                                (double) total * voltage * FeCompat.ratio(false));
            }
            return total;
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return EUMultiP2PPart.this.getSide() == side;
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            return 0;
        }

        @Override
        public long getEnergyStored() {
            long total = 0;

            for (EUMultiP2PPart part : EUMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    try {
                        total = Math.addExact(total, guard.get().getEnergyStored());
                    } catch (ArithmeticException e) {
                        // combined output's storing more than a long's worth of power, return 0
                        // instead, because otherwise it'll look like it's full, ie storing a long
                        // of power, has a max capacity of a long -> full storage
                        return 0;
                    }
                }
            }

            return total;
        }

        @Override
        public long getEnergyCapacity() {
            long total = 0;

            for (EUMultiP2PPart part : EUMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    try {
                        total = Math.addExact(total, guard.get().getEnergyCapacity());
                    } catch (ArithmeticException e) {
                        // combined output's capacity is more than a long's worth of power, return max
                        // long
                        // instead, because otherwise it'll look like it's full, ie storing a long
                        // of power, has a max capacity of a long -> full storage
                        return Long.MAX_VALUE;
                    }
                }
            }

            return total;
        }

        @Override
        public long getInputAmperage() {
            long total = 0;

            for (EUMultiP2PPart part : EUMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    try {
                        total = Math.addExact(total, guard.get().getInputAmperage());
                    } catch (ArithmeticException e) {
                        // combined outputs want more than a long's worth of power, return max long
                        return Long.MAX_VALUE;
                    }
                }
            }

            return total;
        }

        @Override
        public long getInputVoltage() {
            long maxVoltage = 0;

            for (EUMultiP2PPart part : EUMultiP2PPart.this.getOutputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    maxVoltage = Math.max(maxVoltage, guard.get().getInputVoltage());
                }
            }

            return maxVoltage;
        }
    }

    public class OutputHandler implements IEnergyContainer {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            return 0;
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return false;
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            return 0;
        }

        @Override
        public long getEnergyStored() {
            long total = 0;

            for (EUMultiP2PPart part : EUMultiP2PPart.this.getInputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    try {
                        total = Math.addExact(total, guard.get().getEnergyStored());
                    } catch (ArithmeticException e) {
                        // combined output's storing more than a long's worth of power, return 0
                        // instead, because otherwise it'll look like it's full, ie storing a long
                        // of power, has a max capacity of a long -> full storage
                        return 0;
                    }
                }
            }

            return total;
        }

        @Override
        public long getEnergyCapacity() {
            long total = 0;

            for (EUMultiP2PPart part : EUMultiP2PPart.this.getInputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    try {
                        total = Math.addExact(total, guard.get().getEnergyCapacity());
                    } catch (ArithmeticException e) {
                        // combined output's capacity is more than a long's worth of power, return max
                        // long
                        // instead, because otherwise it'll look like it's full, ie storing a long
                        // of power, has a max capacity of a long -> full storage
                        return Long.MAX_VALUE;
                    }
                }
            }

            return total;
        }

        @Override
        public long getInputAmperage() {
            return 0;
        }

        @Override
        public long getInputVoltage() {
            return 0;
        }
    }

    private static class EmptyHandler implements IEnergyContainer {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            return 0;
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return false;
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            return 0;
        }

        @Override
        public long getEnergyStored() {
            return 0;
        }

        @Override
        public long getEnergyCapacity() {
            return 0;
        }

        @Override
        public long getInputAmperage() {
            return 0;
        }

        @Override
        public long getInputVoltage() {
            return 0;
        }
    }
}

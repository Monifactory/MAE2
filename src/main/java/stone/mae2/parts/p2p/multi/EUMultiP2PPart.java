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

import java.util.List;

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

    public class InputHandler implements IEnergyContainer {

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            int total = 0;

            final int outputTunnels = EUMultiP2PPart.this.getOutputs().size();

            if (outputTunnels == 0 | voltage == 0) {
                return 0;
            }

            long toSend = amperage;

            for (EUMultiP2PPart target : EUMultiP2PPart.this.getOutputs()) {
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
                        // instead, because otherwise it'll look like it'll look full, ie storing a long
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
                        // instead, because otherwise it'll look like it'll look full, ie storing a long
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
                        // instead, because otherwise it'll look like it'll look full, ie storing a long
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
                        // instead, because otherwise it'll look like it'll look full, ie storing a long
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

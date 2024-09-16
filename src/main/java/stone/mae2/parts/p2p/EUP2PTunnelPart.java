package stone.mae2.parts.p2p;

import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.Direction;

import stone.mae2.MAE2;

import java.util.List;

public class EUP2PTunnelPart extends CapabilityP2PTunnelPart<EUP2PTunnelPart, IEnergyContainer> {
	private static final P2PModels MODELS = new P2PModels(MAE2.toKey("part/p2p/p2p_tunnel_eu"));
	private static final IEnergyContainer EMPTY_HANDLER = new EmptyHandler();
	
    public EUP2PTunnelPart(IPartItem<?> partItem) {
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
        	// TODO tax the power somehow
            int total = 0;

            final int outputTunnels = EUP2PTunnelPart.this.getOutputs().size();

            if (outputTunnels == 0 | voltage == 0) {
                return 0;
            }

            long toSend = voltage;

            for (EUP2PTunnelPart target : EUP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IEnergyContainer output = capabilityGuard.get();
                    final long received = output.acceptEnergyFromNetwork(target.getSide().getOpposite(), toSend, amperage);

                    toSend -= received;
                    total += received;
                    if (toSend == 0)
                    {
                        break;
                    }
                }
            }

            EUP2PTunnelPart.this
                .queueTunnelDrain(PowerUnits.FE,
                    FeCompat
                        .toFeBounded(total * amperage, FeCompat.ratio(false),
                            Integer.MAX_VALUE));
            return total;
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return EUP2PTunnelPart.this.getSide() == side;
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            return 0;
        }

        @Override
        public long getEnergyStored() {
            long total = 0;

            
            for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
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

            
            for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard guard = part.getAdjacentCapability()) {
                    try {
                        total = Math.addExact(total, guard.get().getEnergyCapacity());
                    } catch (ArithmeticException e) {
                        // combined output's capacity is more than a long's worth of power, return max long
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

            
            for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
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

            
            for (EUP2PTunnelPart part : EUP2PTunnelPart.this.getOutputs()) {
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
			EUP2PTunnelPart input = EUP2PTunnelPart.this.getInput();
			if (input != null)
					return input.getAdjacentCapability().get().getEnergyStored();
			else
				return 0;
		}

		@Override
		public long getEnergyCapacity() {
			EUP2PTunnelPart input = EUP2PTunnelPart.this.getInput();
			if (input != null)
					return input.getAdjacentCapability().get().getEnergyCapacity();
			else
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

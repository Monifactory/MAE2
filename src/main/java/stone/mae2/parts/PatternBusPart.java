package stone.mae2.parts;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.parts.automation.UpgradeablePart;

public class PatternBusPart extends UpgradeablePart implements IGridTickable, IViewCellStorage {

    public PatternBusPart(IPartItem<? extends PatternBusPart> partItem) {
        super(partItem);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.getConfigManager().registerSetting(Settings.
    }

	@Override
	public void getBoxes(IPartCollisionHelper bch) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBoxes'");
	}

	@Override
	public InternalInventory getViewCellStorage() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getViewCellStorage'");
	}

	@Override
	public TickingRequest getTickingRequest(IGridNode node) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getTickingRequest'");
	}

	@Override
	public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'tickingRequest'");
	}
}

package stone.mae2.client.gui;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.definitions.AEItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import stone.mae2.menu.PatternBusMenu;

public class PatternBusScreen extends UpgradeableScreen<PatternBusMenu> {
    private final SettingToggleButton<RedstoneMode> rsMode;
    private final SettingToggleButton<YesNo> blockingBtn;
    
    public PatternBusScreen(PatternBusMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.rsMode = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE));
        this.blockingBtn = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.BLOCKING_MODE, YesNo.NO));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.rsMode.set(menu.getRedStoneMode());
        this.rsMode.setVisibility(menu.hasUpgrade(AEItems.REDSTONE_CARD));
    }

    private void toggleRSMode(SettingToggleButton<RedstoneMode> button, boolean backwards) {
        RedstoneMode rs = button.getNextValue(backwards);
        this.menu.setRedStoneMode(rs);
    }
}

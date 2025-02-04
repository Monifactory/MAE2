package stone.mae2.menu;

import appeng.api.config.YesNo;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class PatternBusMenu extends AEBaseMenu {
    @GuiSync(0)
    public YesNo blockingMode = YesNo.NO;
    
    public PatternBusMenu(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

}

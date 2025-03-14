package stone.mae2.menu;

import appeng.api.config.RedstoneMode;
import appeng.api.config.YesNo;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import stone.mae2.parts.PatternBusPart;

public class PatternBusMenu extends UpgradeableMenu<PatternBusPart> {

    public static final MenuType<PatternBusMenu> TYPE = MenuTypeBuilder
        .create(PatternBusMenu::new, PatternBusPart.class)
        .build("pattern_bus");
    
    @GuiSync(3)
    public YesNo blockingMode = YesNo.NO;

    public PatternBusMenu(MenuType<PatternBusMenu> menuType, int id, Inventory ip, PatternBusPart host) {
        super(menuType, id, ip, host);
    }

    @Override
    protected void setupInventorySlots() {
        InternalInventory pattern = this.getHost().getPatternInventory();
        this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, pattern, 0), SlotSemantics.ENCODED_PATTERN);
    }
}

package stone.mae2.item.faulty;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import appeng.items.tools.MemoryCardItem;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import stone.mae2.item.FaultyCardMode;

public class AoEPaste extends FaultyCardMode {
    private static final String RADIUS = "radius";
    
    private final byte radius;

    public AoEPaste() {
        this.radius = 1;
    }
    
    public AoEPaste(CompoundTag tag) {
        this.radius = tag.getByte(RADIUS);
    }

    @Override
    public InteractionResult onItemUse(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof IPartHost partHost) {
            SelectedPart selectedPart = partHost.selectPartWorld(context.getClickLocation());
            if (selectedPart.part != null && selectedPart.side != null && stack.getItem() instanceof IMemoryCard card) {
                CompoundTag data = card.getData(stack);
                int x = selectedPart.side.getStepX() != 0 ? 0 : 1;
                int y = selectedPart.side.getStepY() != 0 ? 0 : 1;
                int z = selectedPart.side.getStepZ() != 0 ? 0 : 1;
                for (int i = -radius; i <= radius; i++) {
                    for (int j = -radius; j <= radius; j++) {
                        for (int k = -radius; k <= radius; k++) {
                            BlockEntity aoeBE = level.getBlockEntity(context.getClickedPos().offset(i * x, j * y, k * z));
                            if (aoeBE instanceof IPartHost aoePartHost) {
                                IPart part = aoePartHost.getPart(selectedPart.side);
                                if (part != null) {
                                    part.importSettings(SettingsFrom.MEMORY_CARD, data, context.getPlayer());
                                }
                            }
                        }
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "aoe_paste";
    }
}

package stone.mae2.item.faulty;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import stone.mae2.MAE2;
import stone.mae2.util.TransHelper;

public class GlobalPaste extends FaultyCardMode {
    @Override
    public ResourceLocation getType() {
        return MAE2.toKey("global_paste");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof IPartHost partHost) {
            SelectedPart selectedPart = partHost.selectPartWorld(context.getClickLocation());
            if (selectedPart.part != null && selectedPart.side != null && stack.getItem() instanceof IMemoryCard card) {
            	CompoundTag data = card.getData(stack);
            	for (IGridNode node : selectedPart.part.getGridNode().getGrid().getMachineNodes(selectedPart.part.getClass())) {
                    ((IPart) node.getOwner()).onActivate(context.getPlayer(), context.getHand(), context.getClickLocation());
            	}
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getTintColor() {
        return AEColor.ORANGE.mediumVariant;
    }

    @Override
    protected Component getName() {
        return Component.translatable(TransHelper.GUI.toKey("faulty", "global"));
    }
}

package stone.mae2.item;

import appeng.items.tools.MemoryCardItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class FaultyMemoryCardItem extends MemoryCardItem {
    public FaultyMemoryCardItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // copying the settings
        if (context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (!level.isClientSide()) {
            return FaultyCardMode.of(stack.getTag()).onItemUse(stack, context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}

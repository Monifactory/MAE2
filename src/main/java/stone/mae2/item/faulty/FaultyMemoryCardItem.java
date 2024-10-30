package stone.mae2.item.faulty;

import appeng.helpers.IMouseWheelItem;
import appeng.items.tools.MemoryCardItem;
import appeng.util.InteractionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import stone.mae2.MAE2;

public class FaultyMemoryCardItem extends MemoryCardItem implements IMouseWheelItem {
    public FaultyMemoryCardItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
    	// copying/special tool use
        if (context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (!level.isClientSide()) {
            return FaultyCardMode.of(stack).onItemUseFirst(stack, context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    
    

    @Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    	ItemStack handStack = player.getItemInHand(hand);
		if (InteractionUtil.isInAlternateUseMode(player)) {
			this.cycleMode(handStack, true);
			return InteractionResultHolder.consume(handStack);
		} else {
			return FaultyCardMode.of(handStack).onItemUse(level, player, hand);
		}
	}

	private void cycleMode(ItemStack cardStack, boolean cycleForward) {
        FaultyCardMode nextMode = FaultyCardMode.cycleMode(FaultyCardMode.of(cardStack), cycleForward);
        nextMode.save(cardStack.getOrCreateTag());	
    }

	public static int getTintColor(ItemStack stack, int index) {
        if (index == 1) {
            return FaultyCardMode.of(stack).getTintColor();
        }
        return 0xFFFFFF;
    }

	@Override
	public void onWheel(ItemStack is, boolean up) {
		this.cycleMode(is, up);
	}
}

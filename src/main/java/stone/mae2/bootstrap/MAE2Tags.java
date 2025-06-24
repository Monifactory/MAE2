package stone.mae2.bootstrap;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;

import stone.mae2.MAE2;

public class MAE2Tags {
    public static final TagKey<Block> CLOUD_CHAMBERS = BlockTags
        .create(MAE2.toKey("cloud_chambers"));

    public static void init(IEventBus bus) {
    }
}

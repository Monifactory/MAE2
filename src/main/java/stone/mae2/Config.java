package stone.mae2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MAE2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue INTERFACE_P2P = BUILDER
            .comment("Whether the Interface P2P is enabled").define("interfaceP2P", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        MAE2.CONFIG.interfaceP2P = INTERFACE_P2P.get();
    }
}

package stone.mae2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import stone.mae2.util.TranslationHelper;

@Mod.EventBusSubscriber(modid = MAE2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // parts
    private static final ForgeConfigSpec.BooleanValue INTERFACE_P2P = BUILDER
        .push("Parts")
        .comment("Whether the Interface P2P is enabled")
        .worldRestart()
        .translation(TranslationHelper.toKey("gui.interfaceP2P"))
        .define("interfaceP2P", true);

    // storage
    private static final ForgeConfigSpec.BooleanValue STORAGE_TIERS_ENABLED = BUILDER
        .pop().push("Storage").comment("Should extra storage tiers be enabled?")
        .worldRestart()
        .translation(TranslationHelper.toKey("gui", "storage_tiers_enabled"))
        .define("storageTiersEnabled", true);

    private static final ForgeConfigSpec.IntValue STORAGE_TIERS = BUILDER
        .comment("How many storage component tiers past 256k there are",
            "This option is ignored if extra tiers are disabled")
        .worldRestart()
        .translation(TranslationHelper.toKey("gui", "extra_storage_tiers"))
        .defineInRange("storageTiers", 16, 1, 16);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        onReload();
    }

    public static void onReload() {

    }

    public static void onLoad() {
        onReload();
        MAE2Config.isInterfaceP2PEnabled = INTERFACE_P2P.get();

        MAE2Config.areExtraTiersEnabled = STORAGE_TIERS_ENABLED.get();
        MAE2Config.extraStorageTiers = STORAGE_TIERS.get();
    }

    @SubscribeEvent
    static void onload(final ModConfigEvent.Loading event) {
        onLoad();
    }

}

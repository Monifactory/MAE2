package stone.mae2.registration;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import stone.mae2.MAE2;
import stone.mae2.util.TranslationHelper;

@Mod.EventBusSubscriber(modid = MAE2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // parts
    private static final ForgeConfigSpec.BooleanValue INTERFACE_P2P;

    // storage
    private static final ForgeConfigSpec.BooleanValue STORAGE_TIERS_ENABLED;
    private static final ForgeConfigSpec.BooleanValue MAX_TIER_ENABLED;

    private static final ForgeConfigSpec.IntValue STORAGE_TIERS;

    public static final ForgeConfigSpec SPEC;

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
        MAE2Config.isMaxTierEnabled = MAX_TIER_ENABLED.get();
        MAE2Config.extraStorageTiers = STORAGE_TIERS.get();
    }

    @SubscribeEvent
    static void onload(final ModConfigEvent.Loading event) {
        onLoad();
    }

    static {
        // parts
        INTERFACE_P2P = BUILDER
                .push("Parts")
                .comment("Whether the Interface P2P is enabled")
                .worldRestart()
                .translation(TranslationHelper.CONFIG.toKey("interfaceP2P"))
                .define("interfaceP2P", true);

        // storage
        STORAGE_TIERS_ENABLED = BUILDER
                .pop().push("Storage")
                .comment("Should any extra storage things (components, craftings storages, etc.) be enabled?")
                .worldRestart()
                .translation(TranslationHelper.CONFIG.toKey("storage_tiers_enabled"))
                .define("storageTiersEnabled", true);

        MAX_TIER_ENABLED = BUILDER
                .comment("Should the MAX tier of storage be enabled?",
                        "MAX tier is literally the best possible allowed by AE2 for each thing",
                        "Technically storage cells could be pushed farther, but 2^63 is enough",
                        "And note that MAX tier crafting cpus break if combined with anything else")
                .worldRestart()
                .translation(TranslationHelper.CONFIG.toKey("max_tier_enabled"))
                .define("maxTierEnabled", true);

        STORAGE_TIERS = BUILDER
                .comment("How many storage tiers past 256k there are")
                .worldRestart()
                .translation(TranslationHelper.CONFIG.toKey("extra_storage_tiers"))
                .defineInRange("storageTiers", 16, 1, 16);

        SPEC = BUILDER.build();
    }
}

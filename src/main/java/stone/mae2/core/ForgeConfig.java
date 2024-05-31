package stone.mae2.core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import stone.mae2.MAE2;
import stone.mae2.util.TransHelper;

@Mod.EventBusSubscriber(modid = MAE2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeConfig {
    private static final ForgeConfigSpec.Builder BUILDER;

    // parts
    public static class Parts {
        private static ForgeConfigSpec.BooleanValue PATTERN_P2P;
    }

    public static class Storage {
        private static ForgeConfigSpec.BooleanValue MAX_TIER;
        private static ForgeConfigSpec.BooleanValue DENSE_COPROCESSERS;
    }
    // storage

    public static final ForgeConfigSpec SPEC;

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        onReload();
    }

    public static void onReload() {

    }

    public static void onLoad() {
        onReload();
        MAE2Config.isInterfaceP2PEnabled = Parts.PATTERN_P2P.get();
        MAE2Config.areDenseCoprocessersEnabled = Storage.DENSE_COPROCESSERS
            .get();
        MAE2Config.isMAXTierEnabled = Storage.MAX_TIER.get();
    }

    @SubscribeEvent
    static void onload(final ModConfigEvent.Loading event) {
        onLoad();
    }

    static
    {
        BUILDER = new ForgeConfigSpec.Builder();
        // Ensures load order just in case java is funky with inner class load
        // order
        Parts.PATTERN_P2P = BUILDER.push("Parts")
            .comment("Enable/Disable the Pattern P2P", "REQUIRES RESTART")
            .translation(TransHelper.CONFIG.toKey("pattern_p2p"))
            .define("patternP2Ps", true);

        BUILDER.pop().push("Storage");

        Storage.DENSE_COPROCESSERS = BUILDER
            .comment(
                "Enable/Disable dense cpu coprocessers (4x, 16x, 64x, 256x threads)",
                "REQUIRES RESTART")
            .translation(TransHelper.CONFIG.toKey("dense_coprocesser"))
            .define("denseCoprocessers", true);
        
        Storage.MAX_TIER = BUILDER.comment(
            "Enable/Disable MAX tier storage stuff (cells, cpus)",
            "More for fun than anything. Most importantly, MAX tier cpu parts will break when combined with other parts",
            "(so just make a cpu of 1 storage and 1 coprocesser)")
            .translation(TransHelper.CONFIG.toKey("max_tier"))
            .define("maxTier", true);

        SPEC = BUILDER.build();
    }

}

package stone.mae2;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MAE2.MODID)
public class MAE2 {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "mae2";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // public static final ConfigHolder CONFIG = new ConfigHolder();
    public static final MAE2Items ITEMS = new MAE2Items();
    public static final MAE2Config CONFIG = new MAE2Config();

    public MAE2() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.init(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
            ForgeConfig.SPEC);

        // Register our mod's ForgeConfigSpec so that Forge can create and load
        // the config file for us
        // TODO figure out configs
        // ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
        // Config.SPEC);


    }

}

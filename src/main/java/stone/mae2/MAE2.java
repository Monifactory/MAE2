package stone.mae2;

import stone.mae2.api.ConfigHolder;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MAE2.MODID)
public class MAE2
{

    // Define mod id in a common place for everything to reference
    public static final String MODID = "mae2";

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ConfigHolder CONFIG = new ConfigHolder();
    public static MAE2Items ITEMS = new MAE2Items();

    public MAE2()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.init(modEventBus);

        // Register ourselves for server and other game events we are interested
        // in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load
        // the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
            Config.SPEC);
    }

    // You can use EventBusSubscriber to automatically register all static
    // methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
        }
    }
}

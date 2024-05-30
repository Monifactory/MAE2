package stone.mae2;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import stone.mae2.registration.ForgeConfig;
import stone.mae2.registration.MAE2Proxy;

import java.nio.file.Path;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MAE2.MODID)
public class MAE2 {
    public static final String MODID = "mae2";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MAE2() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);
        // terrible, but forge doesn't want dynamic item registration for *some*
        // reason
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("mae2-common.toml");
        CommentedConfig config = TomlFormat.instance().createParser()
            .parse(configPath, FileNotFoundAction.READ_NOTHING);
        if (!config.isEmpty())
        {
            ForgeConfig.SPEC.acceptConfig(config);
            ForgeConfig.onLoad();
        }

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();



        DistExecutor.safeRunForDist(() -> MAE2Proxy.Client::new,
            () -> MAE2Proxy.Server::new).init(bus);

    }

}

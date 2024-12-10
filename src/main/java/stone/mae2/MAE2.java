package stone.mae2;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import stone.mae2.bootstrap.MAE2Config;
import stone.mae2.bootstrap.Proxy;
import java.nio.file.Path;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MAE2.MODID)
public class MAE2 {
    public static final String MODID = "mae2";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static MAE2Config CONFIG;

    public MAE2() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(Type.COMMON,
            MAE2Config.SPEC);

        // terrible, but forge doesn't want dynamic item registration for *some*
        // reason
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("mae2-common.toml");
        CommentedConfig config = TomlFormat.instance().createParser()
            .parse(configPath, FileNotFoundAction.READ_NOTHING);
        if (!config.isEmpty())
        {
            MAE2Config.SPEC.acceptConfig(config);
            MAE2Config.onLoad();
        }

        DistExecutor.safeRunForDist(() -> Proxy.Client::new,
                                    () -> Proxy.Server::new).init(bus);
    }

    public static ResourceLocation toKey(String path) {
        return new ResourceLocation(MAE2.MODID, path);
    }

}

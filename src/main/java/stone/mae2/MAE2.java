package stone.mae2;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import stone.mae2.core.Proxy;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MAE2.MODID)
public class MAE2 {
    public static final String MODID = "mae2";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MAE2() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeRunForDist(() -> Proxy.Client::new,
                                    () -> Proxy.Server::new).init(bus);

    }

    public static ResourceLocation toKey(String path) {
        return new ResourceLocation(MAE2.MODID, path);
    }

}

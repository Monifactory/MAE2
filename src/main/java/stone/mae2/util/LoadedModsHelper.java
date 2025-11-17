package stone.mae2.util;

import net.minecraftforge.fml.ModList;

public class LoadedModsHelper {
    public static final boolean isFork = ModList.get().getModContainerById("ae2")
            .get().getModInfo().getVersion().getQualifier().contains("cosmolite");
}
package stone.mae2.util;

import net.minecraftforge.fml.loading.LoadingModList;

public class LoadedModsHelper {
  public static final boolean isFork = LoadingModList.get().getMods().stream()
    .anyMatch(info -> info.getModId().equals("ae2") && info.getVersion().getQualifier() != null && info.getVersion().getQualifier().contains("cosmolite"));
}

package stone.mae2.util;

import net.minecraft.network.chat.Component;
import stone.mae2.MAE2;

public enum TransHelper {
    ITEM("item"), GUI("gui"), CONFIG("gui", "config"), WAILA("waila");

    String namespace;

    TransHelper(String namespace) {
        this.namespace = namespace + '.' + MAE2.MODID + '.';
    }

    TransHelper(String namespace, String subspace) {
        this.namespace = namespace + '.' + MAE2.MODID + '.' + subspace + '.';
    }

    public String toKey(String... keys) {
        return this.namespace + String.join(".", keys);       
    }

    public Component translatable(String path, Object... args) {
        return Component.translatable(this.toKey(path), args);
    }
}

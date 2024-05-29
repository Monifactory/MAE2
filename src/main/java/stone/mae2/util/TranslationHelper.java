package stone.mae2.util;

import stone.mae2.MAE2;

public enum TranslationHelper {
    ITEM("item"),
    GUI("gui"),
    CONFIG("gui.config");
    
    String prefix;

    private TranslationHelper(String group) {
        prefix = group + '.' + MAE2.MODID;
    }
    
    public String toKey(String... keys) {
        return prefix + String.join(".", keys);
       
    }
}

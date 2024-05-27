package stone.mae2.util;

import stone.mae2.MAE2;

public class TranslationHelper {
    StringBuilder builder;
    public static String toKey(String... keys) {
        return MAE2.MODID + '.' + String.join(".", keys);
       
    }
}

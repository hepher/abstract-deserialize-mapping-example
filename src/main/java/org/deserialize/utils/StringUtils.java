package org.deserialize.utils;

public class StringUtils {

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String defaultIfBlank(String str) {
        if (isBlank(str)) {
            return "";
        }

        return str;
    }

    public static String defaultIfBlank(String str, String defaultStr) {
        if (isBlank(str)) {
            return defaultStr;
        }

        return str;
    }
}

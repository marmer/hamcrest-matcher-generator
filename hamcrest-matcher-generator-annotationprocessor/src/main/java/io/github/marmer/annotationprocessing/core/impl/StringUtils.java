package io.github.marmer.annotationprocessing.core.impl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

    public static String capitalize(final String str) {
        if (str == null) {
            return null;
        }
        switch (str.length()) {
            case 0:
                return str;
            case 1:
                return str.toUpperCase();
            default:
                return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }

    public static String uncapitalize(final String str) {
        if (str == null) {
            return null;
        }
        switch (str.length()) {
            case 0:
                return str;
            case 1:
                return str.toLowerCase();
            default:
                return str.substring(0, 1).toLowerCase() + str.substring(1);
        }
    }
}

package com.anishan.commons.util;

public class ThrowUtil {

    public static void runtime(boolean b, String msg) {
        if (b) {
            throw new RuntimeException(msg);
        }
    }

}

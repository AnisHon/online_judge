package com.anishan.commons.util;

import org.springframework.dao.PermissionDeniedDataAccessException;

public class ThrowUtil {

    public static void runtime(boolean b, String msg) {
        if (b) {
            throw new RuntimeException(msg);
        }
    }

    public static void permissionDeny(boolean b, String msg) {
        if (b) {
            throw new PermissionDeniedDataAccessException(msg, new RuntimeException(msg));
        }
    }

}

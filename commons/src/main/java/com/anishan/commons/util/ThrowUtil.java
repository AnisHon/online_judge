package com.anishan.commons.util;

import com.anishan.commons.exception.BusinessException;
import org.springframework.dao.PermissionDeniedDataAccessException;

public class ThrowUtil {

    public static void runtime(boolean b, String msg) {
        if (b) {
            throw new RuntimeException(msg);
        }
    }

    public static void illegalArgument(boolean b, String msg) {
        if (b) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void permissionDeny(boolean b, String msg) {
        if (b) {
            throw new PermissionDeniedDataAccessException(msg, new RuntimeException(msg));
        }
    }

    public static void illegalState(boolean b, String msg) {
        if (b) {
            throw new IllegalStateException(msg);
        }
    }

    public static void businessError(boolean b, String msg) {
        if (b) {
            throw new BusinessException(msg);
        }
    }

}

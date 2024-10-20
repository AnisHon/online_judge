package com.anishan.user.util;

import com.anishan.user.domain.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtil {

    public static LoginUser getLoginUser() {
        return ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    public static Long getUserId() {
        return getLoginUser().getUser().getUserId();
    }
}

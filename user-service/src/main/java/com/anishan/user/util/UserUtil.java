package com.anishan.user.util;

import com.anishan.api.domain.LoginUser;
import com.anishan.api.util.AuthUtil;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtil {

    public static LoginUser getLoginUser() {
        return AuthUtil.getContextUser();
    }

    public static Long getUserId() {
        return getLoginUser().getUser().getUserId();
    }
}

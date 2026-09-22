package com.anishan.user.config;

import com.anishan.commons.util.JwtUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Browser-facing settings for the refresh cookie.
 *
 * The default path matches the public Gateway path (the browser does not see the
 * StripPrefix rewrite). Production should enable secure cookies; local HTTP
 * development must keep it disabled or the browser will silently drop the cookie.
 */
@Data
@Component
@ConfigurationProperties(prefix = "oj.auth.refresh-cookie")
public class RefreshCookieProperties {

    private String name = "oj_refresh";
    private String path = "/api/user-api/auth";
    private String domain;
    private boolean secure = false;
    private String sameSite = "Lax";
    private long maxAgeSeconds = JwtUtil.REFRESH_EXPIRE_DAYS * 24L * 60L * 60L;
}

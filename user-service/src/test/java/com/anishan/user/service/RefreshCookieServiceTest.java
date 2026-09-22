package com.anishan.user.service;

import com.anishan.user.config.RefreshCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshCookieServiceTest {

    @Test
    void cookieIsHttpOnlyScopedAndReadableByTheServerOnly() {
        RefreshCookieProperties properties = new RefreshCookieProperties();
        RefreshCookieService service = new RefreshCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.write(response, "opaque-refresh-value");

        String header = response.getHeader("Set-Cookie");
        assertTrue(header.contains("oj_refresh=opaque-refresh-value"));
        assertTrue(header.contains("Path=/api/user-api/auth"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        assertTrue(header.contains("Max-Age=604800"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("oj_refresh", "opaque-refresh-value"));
        assertEquals("opaque-refresh-value", service.read(request));
    }

    @Test
    void clearUsesTheSamePathAndExpiresImmediately() {
        RefreshCookieProperties properties = new RefreshCookieProperties();
        RefreshCookieService service = new RefreshCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clear(response);

        String header = response.getHeader("Set-Cookie");
        assertTrue(header.contains("oj_refresh=;"));
        assertTrue(header.contains("Path=/api/user-api/auth"));
        assertTrue(header.contains("Max-Age=0"));
    }
}

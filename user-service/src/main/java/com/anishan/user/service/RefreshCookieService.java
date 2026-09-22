package com.anishan.user.service;

import com.anishan.user.config.RefreshCookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

/** Reads/writes the refresh credential without ever exposing it to JSON or JavaScript. */
@Service
@RequiredArgsConstructor
public class RefreshCookieService {

    private final RefreshCookieProperties properties;

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (properties.getName().equals(cookie.getName())) {
                return StringUtils.hasText(cookie.getValue()) ? cookie.getValue() : null;
            }
        }
        return null;
    }

    public void write(HttpServletResponse response, String value) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(value, properties.getMaxAgeSeconds()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getName(), value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(properties.getPath())
                .maxAge(Duration.ofSeconds(Math.max(maxAgeSeconds, 0)));
        if (StringUtils.hasText(properties.getDomain())) {
            builder.domain(properties.getDomain());
        }
        return builder.build();
    }
}

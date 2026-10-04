package com.anishan.api.util;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/** Negative permissions are an explicit role assignment, never an implicit grant. */
public final class AccountPolicy {
    public static final String SOLUTION_DENY = "policy:solution:deny";
    public static final String COMMENT_DENY = "policy:comment:deny";
    public static final String AVATAR_DENY = "policy:avatar:deny";
    public static final String AVATAR_RESET = "user:user:reset-avatar";

    private AccountPolicy() {}

    public static boolean isDenyPermission(String permission) {
        return permission != null && permission.startsWith("policy:") && permission.endsWith(":deny");
    }

    public static boolean hasAuthority(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
    }

    public static Long requireAllowed(String denyPermission) {
        Long userId = AuthUtil.getUserId();
        if (userId == null || hasAuthority(denyPermission)) {
            throw new AccessDeniedException("当前账号不能执行此操作");
        }
        return userId;
    }

    public static void requireAllowedActor(String denyPermission, Long userId) {
        if (!Objects.equals(requireAllowed(denyPermission), userId)) {
            throw new AccessDeniedException("不能以其他用户身份执行操作");
        }
    }

    public static void requireAuthority(String permission) {
        if (AuthUtil.getUserId() == null || !hasAuthority(permission)) {
            throw new AccessDeniedException("没有执行此操作的权限");
        }
    }
}

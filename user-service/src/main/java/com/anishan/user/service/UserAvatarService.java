package com.anishan.user.service;

import com.anishan.api.file.FileOperation;
import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.util.ThrowUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAvatarService {
    private final SysUserService sysUserService;
    private final FileOperation fileOperation;

    /** Moderation reset has its own positive permission and can target a restricted user. */
    public void resetAvatar(Long userId) {
        AccountPolicy.requireAuthority(AccountPolicy.AVATAR_RESET);
        ThrowUtil.illegalArgument(userId == null || !sysUserService.existsId(userId), "用户不存在");
        fileOperation.deleteFile("avatar/" + userId + "/default_avatar");
        if (fileOperation.fileExists("avatar/" + userId + "/default_avatar")) {
            throw new IllegalStateException("头像重置失败");
        }
    }
}

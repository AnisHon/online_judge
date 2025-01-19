package com.anishan.user.config;

import com.anishan.commons.enumeration.CaptchaCodeType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oj.config")
public class UserConfig {

    private Long checkInAward;
    private Long checkMaxAward;
    private float acProblemAwardRate;
    private Long defaultRoleId;

    /**
     *
     */
    private String defaultPassword;

    /**
     * 验证码类型
     */
    private CaptchaCodeType captchaType = CaptchaCodeType.GIF;

    /**
     * Root账户密码
     */
    private String rootPassword = "4nX7rcIMdv";

}

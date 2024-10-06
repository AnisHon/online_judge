package com.anishan.user.util;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.mail.MailUtil;
import com.anishan.api.entity.LoginUser;
import com.anishan.api.util.JwtUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class EmailSender {



    private static final int SIZE = 6;
    private static final String RANDOM_BASE = "1234567890";
    private static final String SUBJECT = "OJ网 验证码";

    private static String getRandomCode() {
        return new RandomGenerator(RANDOM_BASE, SIZE).generate();
    }

    @NotNull
    @Contract(pure = true)
    private static String getContent(String code) {
        return "您的验证码是:" + code;
    }





    public static String sendEmailCodeAsync(String email) {
        final String randomCode = getRandomCode();
        ThreadUtil.execAsync(() -> {
            MailUtil.send(email, SUBJECT, getContent(randomCode), false);
            System.out.println("Test");
            System.out.println(email);
            System.out.println(randomCode);
        });
        return randomCode;
    }




}

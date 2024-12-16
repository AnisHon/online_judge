package com.anishan.user.util;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.mail.MailUtil;
import com.anishan.commons.util.ThrowUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EmailSender {

    private static final int SIZE = 6;
    private static final String RANDOM_BASE = "1234567890";
    private static final String SUBJECT = "OJ网 验证码";

    private final TemplateEngine templateEngine;


    private static String getRandomCode() {
        return new RandomGenerator(RANDOM_BASE, SIZE).generate();
    }


    private String getHtmlContent(String code) {
        Context context = new Context();
        context.setVariable("code", code);
        return templateEngine.process("email_template", context);
    }

    @NotNull
    @Contract(pure = true)
    private static String getContent(String code) {
        return "您的验证码是:" + code;
    }





    public static String sendEmailCodeAsync(String email) {
        boolean illegalEmail =  email == null || email.isEmpty();
        ThrowUtil.illegalArgument(illegalEmail, "邮箱不存在/非法邮箱");

        final String randomCode = getRandomCode();
        ThreadUtil.execAsync(() -> {
            MailUtil.send(email, SUBJECT, getContent(randomCode), false);
        });
        return randomCode;
    }

    public String sendEmailCode(String email) {
        boolean illegalEmail =  email == null || email.isEmpty();
        ThrowUtil.illegalArgument(illegalEmail, "邮箱不存在/非法邮箱");

        final String randomCode = getRandomCode();
        ThreadUtil.execAsync(() -> {
            String htmlContent = getHtmlContent(randomCode);
            MailUtil.send(email, SUBJECT, htmlContent, true);
        });


        return randomCode;
    }




}

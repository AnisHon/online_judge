package com.anishan.user.service.impl;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EmailServiceImpl implements EmailService {

    private static final int SIZE = 6;
    private static final String RANDOM_BASE = "1234567890";
    private static final String SUBJECT = "OJ网 验证码";


    private static final RandomGenerator randomGenerator;
    private final TemplateEngine templateEngine;

    private final static ExecutorService executorService;

    static {
        executorService = ThreadUtil.newFixedExecutor(4, Integer.MAX_VALUE, "email", false);
        randomGenerator = new RandomGenerator(RANDOM_BASE, SIZE);
    }


    private static String getRandomCode() {
        return randomGenerator.generate();
    }


    private String getHtmlContent(String code) {
        Context context = new Context();
        context.setVariable("code", code);
        return templateEngine.process("email_template", context);
    }

    /**
     * 生成一个随机验证码并发送，使用
     * @param email 邮箱
     * @return 随机验证码
     */
    @Override
    public String sendEmailCode(String email) {
        boolean illegalEmail = StrUtil.isEmpty(email);
        ThrowUtil.businessError(illegalEmail, "邮箱不存在/非法邮箱");

        final String randomCode = getRandomCode();
        Runnable task = () -> MailUtil.send(email, SUBJECT, getHtmlContent(randomCode), true);

        executorService.submit(task);
        return randomCode;
    }




}

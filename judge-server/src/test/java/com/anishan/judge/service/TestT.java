package com.anishan.judge.service;


import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.judge.JudgeRun;
import com.anishan.judge.util.StringUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.StringReader;

@SpringBootTest
public class TestT {

    @Resource
    JudgeService judgeService;

    @Resource
    LanguageConfigLoader languageConfigLoader;

    @Resource
    JudgeRun judgeRun;

    @Test
    @SneakyThrows
    public void test() {

        String str1 = "123    \n123123                  \n";
        String str2 = "123 \r\n123123 ";

        StringReader reader = new StringReader(str1);
        StringReader reader1 = new StringReader(str2);
        boolean b = StringUtils.contentEqualsIgnoreBlankAndEOL(reader, reader1);
        System.out.println(b);

    }

}

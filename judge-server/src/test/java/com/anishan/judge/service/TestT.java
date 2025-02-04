package com.anishan.judge.service;


import cn.hutool.core.io.IoUtil;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.judge.JudgeRun;
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

        String str1 = "123\n123123 \n";
        String str2 = "123\n123123 ";

        StringReader reader = new StringReader(str1);
        StringReader reader1 = new StringReader(str2);
        boolean b = IoUtil.contentEqualsIgnoreEOL(reader1, reader);
        System.out.println(b);

        


    }

}

package com.anishan.judge.service;


import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.judge.JudgeRun;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

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

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo
                .setUserId(4L)
                .setUuid("123")
                .setContestId(null)
                .setProblemId(1880247920550653953L)
                .setLanguage("C++")
                .setCode("#include <iostream>\n" +
                        "\n" +
                        "int main() {\n" +
                        "\tint a;\n" +
                        "\tstd::cin >> a;\n" +
                        "\tstd::cout << a;\n" +
                        "\treturn 0;\n" +
                        "}")
                .setLanguage("C++")
                .setTimeLimit(123123123213123L)
                .setMemoryLimit(12312323123L)
                .setStackLimit(123123123);


        RunTestInfo runTestInfo = new RunTestInfo();
        runTestInfo.setStdin("1 2 3");
        runTestInfo.setCode(judgeInfo.getCode());
        runTestInfo.setLanguage("C++");
        runTestInfo.setUserId(123L);
        runTestInfo.setUuid("123");

        System.out.println(judgeRun.judgeTest(runTestInfo));



    }

}

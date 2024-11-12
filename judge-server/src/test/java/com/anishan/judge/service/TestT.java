package com.anishan.judge.service;

import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.domain.entity.LanguageConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class TestT {

    @Resource
    JudgeService judgeService;

    @Resource
    LanguageConfigLoader languageConfigLoader;

    @Test
    @SneakyThrows
    public void test() {

        LanguageConfig python3 = languageConfigLoader.getLanguageConfigByName("Python3");
        System.out.println(python3.getRunCommand());

//        String code = "i = input()\n" +
//                "a ,b = i.split(\" \")\n" +
//                "print(int(a) + int(b))";
//
//        JudgeMessage message = new JudgeMessage();
//        message.setCode(code);
//        OjProblemCaseVo e1 = new OjProblemCaseVo();
//        e1.setInput("1 1");
//        e1.setOutput("2\n");
//        e1.setScore(BigDecimal.ONE);
//        message.setCases(List.of(e1));
//
//        message.setLanguage("Python3");
//
//        message.setMemoryLimit(100000L);
//        message.setTimeLimit(100000L);
//        message.setStackLimit(100000);
//
//        judgeService.judge(message);

    }

}

package com.anishan.judge.service;

import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Slf4j
public class JudgeServiceTest {


    @Resource
    JudgeService judgeService;

    @Test
    @SneakyThrows
    public void test() {
        String code = "#include <stdio.h>\n" +
                "int main()\n" +
                "{\n" +
                "   printf(\"Hello, World!\");\n" +
                "   return 0;\n" +
                "}";
        OjProblemCaseVo e1 = new OjProblemCaseVo();
        e1.setScore(BigDecimal.TEN);
        e1.setInput("");
        e1.setOutput("Hello, World!");
        JudgeMessage judgeMessage = new JudgeMessage(
                1L,
                1001L,
                null,
                1L,
                code,
                "C",
                1000L,
                1000L,
                128,
                List.of(e1),
                BigDecimal.ONE
        );
        JudgeScore judge = judgeService.judge(judgeMessage);

        log.debug(judge.toString());


    }


}

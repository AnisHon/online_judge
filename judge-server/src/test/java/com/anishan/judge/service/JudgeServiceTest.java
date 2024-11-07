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
    }


}

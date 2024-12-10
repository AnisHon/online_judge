package com.anishan.problem;

import com.anishan.problem.domain.dto.DetailProblemDto;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.util.ProblemUploadUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@SpringBootTest
public class TestT {


    @Resource
    ProblemUploadUtil problemUploadUtil;

    @Resource
    ObjectMapper mapper;

    @SneakyThrows
    @Test
    public void test() {
        Class<Contest> contestClass = Contest.class;

        Method[] declaredMethods = contestClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {

        }


    }


}

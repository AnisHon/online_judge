package com.anishan.problem;

import com.anishan.problem.domain.dto.DetailProblemDto;
import com.anishan.problem.util.ProblemUploadUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
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
        String json = "{\n" +
                "            \"title\": \"A+B 输入输出练习I\",\n" +
                "            \"label\": \"待定\",\n" +
                "            \"time_limit\": \"2\",\n" +
                "            \"memory_limit\": \"32\",\n" +
                "            \"description\": \"\\n\\t你的任务是计算a+b。这是为了acm初学者专门设计的题目。你肯定发现还有其他题目跟这道题的标题类似，这些问题也都是专门为初学者提供的。\\n\\n\",\n" +
                "            \"input\": \"\\n\\t输入包含一系列的a和b对，通过空格隔开。一对a和b占一行。\\n\\n\",\n" +
                "            \"output\": \"\\n\\t对于输入的每对a和b，你需要依次输出a、b的和。\\n\\n\\n\\t如对于输入中的第二对a和b，在输出中它们的和应该也在第二行。\\n\\n\",\n" +
                "            \"sample_input\": \"1 5\",\n" +
                "            \"sample_output\": \"6\",\n" +
                "            \"test_data\": [\n" +
                "                {\n" +
                "                    \"input\": \"0 0\",\n" +
                "                    \"output\": \"0\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"-1 1\",\n" +
                "                    \"output\": \"0\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"1 -1\",\n" +
                "                    \"output\": \"0\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"1949 15390\",\n" +
                "                    \"output\": \"17339\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"22139 30520\",\n" +
                "                    \"output\": \"52659\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"16185 15229\",\n" +
                "                    \"output\": \"31414\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"-4362 7115\",\n" +
                "                    \"output\": \"2753\\r\\n\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"input\": \"-23038 -16881\",\n" +
                "                    \"output\": \"-39919\\r\\n\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"solution\": \"#include <iostream>\\nusing namespace std;\\nint main() {\\n    int a,b;\\n    while (cin >> a >> b) {\\n        cout << a+b << endl;\\n    }\\n\\treturn 0;\\n}\\n\"\n" +
                "        }";


        FileInputStream fileInputStream = new FileInputStream("E:\\free_1.json");

        JsonNode jsonNode = mapper.readTree(json);
        List<DetailProblemDto> x = problemUploadUtil.jsonToProblem(fileInputStream);
        System.out.println(x);
        System.out.println(x.size());
    }


}

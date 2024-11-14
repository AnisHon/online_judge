package com.anishan.problem.util;

import com.anishan.commons.e.Difficulty;
import com.anishan.commons.e.ProblemAuth;
import com.anishan.commons.e.ProblemType;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.dto.ChoiceFillAnswersDto;
import com.anishan.problem.domain.dto.DetailProblemDto;
import com.anishan.problem.domain.dto.OjProblemDto;
import com.anishan.problem.domain.dto.ProblemDto;
import com.anishan.problem.domain.entity.OjProblemCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemUploadUtil {



    private final ObjectMapper objectMapper;

    private ChoiceFillAnswersDto toChoiceFillAnswers(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode answerText = node.get("answer_text");
        JsonNode isCorrect = node.get("is_correct");
        JsonNode blankIndex = node.get("blank_index");
        JsonNode score = node.get("score");

        ThrowUtil
                .illegalArgument(answerText == null, "文本不能为空");
        ThrowUtil.illegalArgument(blankIndex == null, "索引不能为空");

        ChoiceFillAnswersDto result = new ChoiceFillAnswersDto()
                .setAnswerText(answerText.asText())
                .setBlankIndex(blankIndex.asInt())
                .setIsCorrect(isCorrect != null && isCorrect.asBoolean(false));
        ThrowUtil.illegalArgument(result.getIsCorrect() && score == null, "缺少分数");
        result.setScore(new BigDecimal(score.asText("1")));

        return result;
    }

    private OjProblemCase toOjProblemCase(JsonNode node) {
        if (node == null) {
            return null;
        }


        JsonNode input = node.get("input");
        JsonNode output = node.get("output");
        JsonNode score = node.get("score");
        ThrowUtil.illegalArgument(input == null, "文本不能为空");
        ThrowUtil.illegalArgument(output == null, "索引不能为空");


        return new OjProblemCase()
                .setInput(input.asText().replace("\r\n", "\n"))
                .setOutput(output.asText().replace("\r\n", "\n"))
                .setScore(new BigDecimal(score == null ? "1" : score.asText("1")));

    }


    private List<OjProblemCase> toCases(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }

        ArrayNode array = (ArrayNode) node;

        ArrayList<OjProblemCase> cases = new ArrayList<>();

        array.forEach(jsonNode -> {
            OjProblemCase ojProblemCase = toOjProblemCase(jsonNode);
            if (ojProblemCase != null) {
                cases.add(ojProblemCase);
            }
        });

        return cases;

    }


    private List<ChoiceFillAnswersDto> toAnswers(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }

        ArrayNode array = (ArrayNode) node;

        ArrayList<ChoiceFillAnswersDto> cases = new ArrayList<>();

        array.forEach(jsonNode -> {
            ChoiceFillAnswersDto ojProblemCase = toChoiceFillAnswers(jsonNode);
            if (ojProblemCase != null) {
                cases.add(ojProblemCase);
            }
        });

        return cases;
    }

    private OjProblemDto toOjProblemDto(JsonNode node) {
        if (node == null) {
            return null;
        }
        JsonNode timeLimit = node.get("time_limit");
        JsonNode memoryLimit = node.get("memory_limit");
        JsonNode input = node.get("input");
        JsonNode output = node.get("output");
        JsonNode sampleInput = node.get("sample_input");
        JsonNode sampleOutput = node.get("sample_output");
        JsonNode difficulty = node.get("difficulty");
        JsonNode stackLimit = node.get("stack_limit");

        ThrowUtil.illegalArgument(timeLimit == null, "时间限制不能为空");
        ThrowUtil.illegalArgument(memoryLimit == null, "内存限制不能为空");
        ThrowUtil.illegalArgument(input == null, "输入描述不能为空");
        ThrowUtil.illegalArgument(output == null, "输出描述不能为空");
        ThrowUtil.illegalArgument(sampleInput == null, "输人示例不能为空");
        ThrowUtil.illegalArgument(sampleOutput == null, "输出示例不能为空");

        OjProblemDto result = new OjProblemDto()
                .setTimeLimit(timeLimit.asInt() * 1000) // ms
                .setMemoryLimit(memoryLimit.asInt() * 1024) // kb
                .setInput(input.asText())
                .setOutput(output.asText())
                .setInputExample(sampleInput.asText())
                .setOutputExample(sampleOutput.asText())
                .setDifficulty(Difficulty.valueOf(difficulty == null ? Difficulty.Unknown.name() : difficulty.asText(Difficulty.Unknown.name())))
                .setStackLimit(stackLimit == null ? 128 : stackLimit.asInt(128));

        ThrowUtil.illegalArgument(result.getTimeLimit() <= 0, "实现限制不能为0或空");
        ThrowUtil.illegalArgument(result.getMemoryLimit() <= 0, "实现限制不能为0或空");
        ThrowUtil.illegalArgument(result.getStackLimit() <= 0, "实现限制不能为0或空");
        return result;
    }

    private ProblemDto toProblem(JsonNode node) {
        if (node== null) {
            return null;
        }
        ProblemDto problemDto = new ProblemDto();

        JsonNode title = node.get("title");
        JsonNode description = node.get("description");
        JsonNode source = node.get("source");
        JsonNode type = node.get("type");
        JsonNode hint = node.get("hint");
        JsonNode auth = node.get("auth");


        ThrowUtil.illegalArgument(title== null, "题目不能为空");
        ThrowUtil.illegalArgument(description == null, "题目描述不能为空");

        problemDto
                .setTitle(title.asText())
                .setDescription(description.asText())
                .setSource(source == null ? null : source.asText(null))
                .setHint(hint == null ? null : hint.asText(null))
                .setAuth(ProblemAuth.valueOf(auth == null ? ProblemAuth.Public.name() : auth.asText(ProblemAuth.Public.name())))
                .setType(ProblemType.valueOf(type == null ? ProblemType.OJ.name() : type.asText(ProblemType.OJ.name())));


        return problemDto;
    }

    public DetailProblemDto toDetailProblem(JsonNode node) {
        if (node == null || node.isArray()) {
            return null;
        }

        ProblemDto problem = toProblem(node);


        OjProblemDto ojProblemDto = null;
        List<OjProblemCase> cases = null;
        List<ChoiceFillAnswersDto> answers = null;

        if (Objects.requireNonNull(problem.getType()) == ProblemType.OJ) {
            ojProblemDto = toOjProblemDto(node);
            cases = toCases(node.get("test_data"));
        } else {
            answers = toAnswers(node.get("answers"));
        }

        return new DetailProblemDto()
                .setProblem(problem)
                .setOjProblem(ojProblemDto)
                .setCases(cases)
                .setChoices(answers);

    }

    public List<DetailProblemDto> toDetailProblems(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }



        ArrayList<DetailProblemDto> problemList = new ArrayList<>();

        node.forEach(problem -> {
            DetailProblemDto detailProblem = toDetailProblem(problem);
            if (detailProblem != null) {
                problemList.add(detailProblem);
            }

        });
        return problemList;
    }




//
    public List<DetailProblemDto> jsonToProblem(InputStream inputStream) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(inputStream);


        List<DetailProblemDto> problems;
        jsonNode = jsonNode.get("problems");

        ThrowUtil.illegalArgument(jsonNode == null, "JSON文档错误");

        if (jsonNode.isArray()) {
            problems = toDetailProblems(jsonNode);
        } else {
            problems = List.of(toDetailProblem(jsonNode));
        }
        return problems;
    }
}

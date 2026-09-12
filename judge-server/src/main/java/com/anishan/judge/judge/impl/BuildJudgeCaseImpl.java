package com.anishan.judge.judge.impl;

import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.api.file.FileOperation;
import com.anishan.judge.config.JudgeConfig;
import com.anishan.judge.domain.CaseContent;
import com.anishan.judge.domain.JudgeCases;
import com.anishan.judge.judge.BuildJudgeCase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BuildJudgeCaseImpl implements BuildJudgeCase {


    private final JudgeConfig judgeConfig;

    private final FileOperation fileOperation;

    @Value("${oj.minio.bucket-name}")
    private String bucketName;

    private String getPath(String path) {
        return judgeConfig.getMountPath() + "/" + bucketName + "/" + path;
    }

    private BigDecimal getTotalScore(List<OjProblemCase> cases) {
        BigDecimal totalScore = BigDecimal.ZERO;
        for (OjProblemCase item : cases) {
            totalScore = totalScore.add(item.getScore());
        }
        return totalScore;
    }


    private CaseContent buildCaseContent(OjProblemCase item) {
        String input = item.getInput();
        String output = item.getOutput();

        long size = fileOperation.getFileInfo(item.getInput()).getSize();


        return CaseContent
                .builder()
                .caseId(item.getCaseId())
                .score(item.getScore())
                .inPath(getPath(input))
                .outPath(output)         // 输出不需要路径转换
                .outSize(size * 2)       // 最大大小是原 * 2
                .build();
    }

    @Override
    public JudgeCases buildJudgeCases(Long problemId) {
        LambdaQueryWrapper<OjProblemCase> wrapper = Wrappers.lambdaQuery(OjProblemCase.class)
                .eq(OjProblemCase::getProblemId, problemId);
        List<OjProblemCase> cases = Db.list(wrapper);

        List<CaseContent> caseContents = new ArrayList<>();

        cases.forEach(caseContent -> caseContents.add(buildCaseContent(caseContent)));

        BigDecimal totalScore = getTotalScore(cases);

        return JudgeCases
                .builder()
                .totalScore(totalScore)
                .caseContents(caseContents)
                .build();
    }



}

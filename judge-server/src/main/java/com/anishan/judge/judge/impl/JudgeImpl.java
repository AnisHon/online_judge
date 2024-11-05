package com.anishan.judge.judge.impl;

import cn.hutool.json.JSONArray;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.judge.Judge;
import com.anishan.judge.judge.SandboxRun;
import com.anishan.judge.util.JudgeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeImpl implements Judge {

    private final SandboxRun sandboxRun;
    private final LanguageConfigLoader languageConfigLoader;
    private final ObjectMapper objectMapper;

    /**
     * 运行
     * @param fileId 沙箱题目ID
     * @param languageConfig 语言配置
     * @param input 输入配置
     * @param maxTime 最大时间
     * @param maxMemory 最大内存
     * @param maxStack 最大栈内存
     * @return GO-Judge判题机的相应结果
     * @throws SystemError 无法连接的时候会抛出异常
     */
    private com.anishan.api.client.gojudge.domain.RunResult doJudge(
            String fileId,
            LanguageConfig languageConfig,
            String input, Long maxTime,
            Long maxMemory,
            Integer maxStack
    ) throws SystemError {

        maxTime = Math.min(maxTime, languageConfig.getMaxCpuTime());
        maxMemory = Math.min(maxMemory, languageConfig.getMaxMemory());
        LanguageConfig languageConfigByName = languageConfigLoader.getLanguageConfigByName("C++");
        JSONArray resultNode = sandboxRun.testCase(
                JudgeUtils.translateCommandline(languageConfigByName.getRunCommand()),
                languageConfigByName.getRunEnvs(),
                null,
                input,
                maxTime,
                maxMemory,
                1024L * 200L,
                maxStack,
                languageConfigByName.getExeName(),
                fileId,
                null,
                false,
                null,
                null
        );
        return objectMapper.convertValue(resultNode.get(0), com.anishan.api.client.gojudge.domain.RunResult.class);
    }


    @Override
    public List<com.anishan.api.client.gojudge.domain.RunResult> judgeAll(
            String fileId,
            LanguageConfig languageConfig,
            Long memLimit,
            Long timeLimit,
            Integer stackLimit,
            List<String> cases) throws SystemError {
        ArrayList<RunResult> results = new ArrayList<>(cases.size());
        for (String case_ : cases) {
            results.add(
                    doJudge(
                            fileId,
                            languageConfig,
                            case_,
                            timeLimit,
                            memLimit,
                            stackLimit
                    ));
        }
        return results;
    }
}

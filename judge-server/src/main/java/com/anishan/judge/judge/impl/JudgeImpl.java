package com.anishan.judge.judge.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.domain.JudgeContent;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.judge.Judge;
import com.anishan.judge.judge.SandboxRun;
import com.anishan.judge.util.JudgeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeImpl implements Judge {

    private final SandboxRun sandboxRun;
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
    @Override
    public RunResult doJudge(
            String fileId,
            LanguageConfig languageConfig,
            String input, Long maxTime,
            Long maxMemory,
            Integer maxStack
    ) throws SystemError {

        maxTime = Math.min(maxTime, languageConfig.getMaxCpuTime());
        maxMemory = Math.min(maxMemory, languageConfig.getMaxMemory());
        JSONArray resultNode = sandboxRun.testCase(
                JudgeUtils.translateCommandline(languageConfig.getRunCommand()),
                languageConfig.getRunEnvs(),
                null,
                input,
                maxTime,
                maxMemory,
                1024L * 200L,
                maxStack,
                languageConfig.getExeName(),
                fileId,
                null,
                true,
                null,
                null
        );

        return objectMapper.convertValue(resultNode.get(0), RunResult.class);
    }


    @Override
    public RunResult doJudge(JudgeContent content) throws SystemError {
        LanguageConfig languageConfig = content.getLanguageConfig();
        Long maxTime = Math.min(content.getMaxTime(), languageConfig.getMaxRealTime() * 1000);
        Long maxMemory = Math.min(content.getMaxTime(), languageConfig.getMaxMemory());
        List<String> args = JudgeUtils.translateCommandline(languageConfig.getRunCommand());
        List<String> env = languageConfig.getRunEnvs();
        Long maxOutputSize = Math.max(content.getMaxOutputSize(), 32 * 1024 * 1024L);

        JSONArray result = sandboxRun.testCase(
                args,
                env,
                content.getTestCasePath(),
                content.getTestCaseContent(),
                maxTime,
                maxMemory,
                maxOutputSize,
                content.getMaxStack(),
                languageConfig.getExeName(),
                content.getFileId(),
                content.getFileContent(),
                content.isFileIO(),
                content.getIoReadFileName(),
                content.getIoWriteFileName()
        );
        return objectMapper.convertValue(result.get(0), RunResult.class);
    }

    @Override
    public List<RunResult> judgeAll(
            String fileId,
            LanguageConfig languageConfig,
            Long memLimit,
            Long timeLimit,
            Integer stackLimit,
            List<String> cases) throws SystemError {
        ArrayList<RunResult> results = new ArrayList<>(cases.size());

        // 没有测试用例，很抽象，运行一下，应付一下，反手甩一个RE
        if (CollectionUtil.isEmpty(cases)) {
            results.add(
                    doJudge(
                            fileId,
                            languageConfig,
                            "",
                            timeLimit,
                            memLimit,
                            stackLimit
                    ));
        } else {
            // 正常运行
            for (String case_ : cases) {
                results.add(
                        doJudge(
                                fileId,
                                languageConfig,
                                Objects.requireNonNullElse(case_, ""), // 是null就返回空串
                                timeLimit,
                                memLimit,
                                stackLimit
                        ));
            }
        }

        return results;
    }
}

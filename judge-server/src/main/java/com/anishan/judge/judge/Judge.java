package com.anishan.judge.judge;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.domain.JudgeContent;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.SystemError;

import java.util.List;

public interface Judge {
    RunResult doJudge(
            String fileId,
            LanguageConfig languageConfig,
            String input, Long maxTime,
            Long maxMemory,
            Integer maxStack
    ) throws SystemError;

    RunResult doJudge(JudgeContent content) throws SystemError;

    List<RunResult> judgeAll(String fileId, LanguageConfig languageConfig, Long memLimit, Long timeLimit, Integer stackLimit, List<String> cases) throws SystemError;
}

package com.anishan.judge.judge;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.SystemError;

import java.util.List;

public interface Judge {
    List<RunResult> judgeAll(String fileId, LanguageConfig languageConfig, Long memLimit, Long timeLimit, Integer stackLimit, List<String> cases) throws SystemError;
}

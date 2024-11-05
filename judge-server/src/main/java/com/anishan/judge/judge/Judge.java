package com.anishan.judge.judge;

import com.anishan.api.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.vo.OjProblemVo;
import com.anishan.judge.entity.LanguageConfig;
import com.anishan.judge.entity.RunResult;
import com.anishan.judge.exception.SystemError;

import java.util.List;

public interface Judge {
    List<RunResult> judgeAll(String fileId, LanguageConfig languageConfig, Long memLimit, Long timeLimit, Integer stackLimit, List<String> cases) throws SystemError;
}

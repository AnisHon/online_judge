package com.anishan.judge.judge;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.domain.entity.LanguageConfig;

public interface JudgeRun {


    RunResult run(String fileId, String stdin, LanguageConfig config);
}

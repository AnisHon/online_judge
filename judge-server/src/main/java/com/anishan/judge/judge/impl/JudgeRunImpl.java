package com.anishan.judge.judge.impl;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.judge.JudgeRun;
import com.anishan.judge.judge.SandboxRun;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeRunImpl implements JudgeRun {

    private final SandboxRun sandboxRun;

    @Override
    public RunResult run(String fileId, String stdin, LanguageConfig config) {
        return null;
    }
}

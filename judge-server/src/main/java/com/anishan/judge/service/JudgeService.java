package com.anishan.judge.service;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;

import java.util.List;

public interface JudgeService {


    String compile(LanguageConfig config, JudgeMessage message) throws CompileError, SystemError, SubmitError;

    List<RunResult> runCases(LanguageConfig config, String fileId, JudgeMessage message, List<String> cases) throws SystemError;

    void deleteFile(String fileId);

    JudgeScore judge(JudgeMessage message) throws SystemError, SubmitError;
}

package com.anishan.judge.service;

import com.anishan.api.client.gojudge.domain.RunResult;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;

import java.util.List;

public interface JudgeService {

    JudgeScore judge(JudgeInfo info, Long submitId) throws SystemError, SubmitError;

    /**
     * 编译文件
     * @param config 语言配置
     * @param message judge消息
     * @return 编译后缓存文件ID
     * @throws CompileError 编译错误，用户程序错误
     * @throws SystemError  链接出错
     * @throws SubmitError  提交失败
     */
    String compile(LanguageConfig config, JudgeMessage message) throws CompileError, SystemError, SubmitError;

    /**
     * 运行程序
     * @param config 语言配置
     * @param fileId 缓存程序ID
     * @param message 判题消息
     * @param cases  运行输入 stdin
     * @return 判题结果
     * @throws SystemError 链接出错
     */
    List<RunResult> runCases(LanguageConfig config, String fileId, JudgeMessage message, List<String> cases) throws SystemError;

    /**
     *  删除文件
     * @param fileId 文件ID
     */
    void deleteFile(String fileId);

    /**
     * 编译-运行-判分-删除文件
     * @param message judge消息
     * @return 返回分数等参数
     * @throws SystemError 链接出错
     * @throws SubmitError 提交失败
     */
    JudgeScore judge(JudgeMessage message) throws SystemError, SubmitError;

    TestResult test(JudgeMessage message) throws SystemError, SubmitError;

    void sendJudgeMessage(JudgeInfo judgeInfo);
}

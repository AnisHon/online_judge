package com.anishan.api.client.gojudge.domain;

import com.anishan.commons.enumeration.JudgeResult;
import lombok.Data;

@Data
public class TestResult {

    private Long userId;
    private JudgeResult judgeResult;
    private String stderr;
    private String stdout;

    public static TestResult fromTestResul(RunResult runResult, JudgeResult result) {

        TestResult testResult;
        switch (result) {
            case RuntimeError:
                testResult = TestResult.runtimeError(runResult);
                break;
            case TimeLimitExceeded:
                testResult = TestResult.timeoutError();
                break;
            case MemoryLimitExceeded:
                testResult = TestResult.memoryError();
                break;
            default:
                testResult = TestResult.accept(runResult);
                break;

        }
        return testResult;
    }

    public static TestResult compileError(String stderr) {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.CompileError);
        testResult.setStderr(stderr);
        return testResult;
    }

    public static TestResult runtimeError(RunResult runResult) {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.RuntimeError);
        testResult.setStderr(runResult.getFiles().getStderr());
        return testResult;
    }


    public static TestResult timeoutError() {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.TimeLimitExceeded);
        return testResult;
    }

    public static TestResult memoryError() {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.MemoryLimitExceeded);
        return testResult;
    }

    public static TestResult accept(RunResult runResult) {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.Accept);
        testResult.setStdout(runResult.getFiles().getStdout());
        return testResult;
    }

}

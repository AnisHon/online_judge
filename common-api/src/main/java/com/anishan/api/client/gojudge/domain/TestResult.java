package com.anishan.api.client.gojudge.domain;

import com.anishan.commons.enumeration.JudgeResult;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TestResult {

    private Long userId;
    private JudgeResult judgeResult;
    private String stderr;
    private String stdout;

    public static TestResult fromTestResul(RunResult runResult, JudgeResult result) {

        TestResult testResult;
        switch (result) {
            case RUNTIME_ERROR:
                testResult = TestResult.runtimeError(runResult);
                break;
            case TIME_LIMIT_EXCEEDED:
                testResult = TestResult.timeoutError();
                break;
            case MEMORY_LIMIT_EXCEEDED:
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
        testResult.setJudgeResult(JudgeResult.COMPILE_ERROR);
        testResult.setStderr(stderr);
        return testResult;
    }

    public static TestResult runtimeError(RunResult runResult) {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.RUNTIME_ERROR);
        testResult.setStderr(runResult.getFiles().getStderr());
        return testResult;
    }


    public static TestResult timeoutError() {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.TIME_LIMIT_EXCEEDED);
        return testResult;
    }

    public static TestResult memoryError() {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.MEMORY_LIMIT_EXCEEDED);
        return testResult;
    }

    public static TestResult accept(RunResult runResult) {
        TestResult testResult = new TestResult();
        testResult.setJudgeResult(JudgeResult.ACCEPT);
        testResult.setStdout(runResult.getFiles().getStdout());
        return testResult;
    }

}

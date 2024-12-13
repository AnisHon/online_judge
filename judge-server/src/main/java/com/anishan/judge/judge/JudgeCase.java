package com.anishan.judge.judge;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 测试用例保存实现接口，需要向ProblemService开放接口
 */
public interface JudgeCase {

    void setCase(Long problemId, Long caseId, String text);

    void setCase(Long problemId, Long caseId, InputStream inputStream);

    List<OutputStream> getCases(Long problemId);

    List<String> getCasesUrl(Long problemId);

    List<String> getCasesAsText(Long problemId);

    void deleteCases(Long problemId, List<Long> caseId);

    void deleteCase(Long problemId, Long caseId);

}

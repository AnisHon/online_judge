package com.anishan.judge.judge;

import com.anishan.commons.enumeration.CaseFileType;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * 测试用例保存实现接口，需要向ProblemService开放接口
 */
public interface JudgeCase {

    void setCase(Long problemId, Long caseId, String input, String output);

    void setCase(Long problemId, Long caseId, InputStream input, InputStream output);

    List<Reader> getCases(Long problemId, CaseFileType fileType);

    List<String> getCaseUrls(Long problemId, CaseFileType fileType);

    List<String> getCasesAsText(Long problemId);

    void deleteCases(Long problemId, List<Long> caseId);

    void deleteCase(Long problemId, Long caseId);

}

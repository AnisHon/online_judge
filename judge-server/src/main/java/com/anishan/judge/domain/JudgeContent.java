package com.anishan.judge.domain;

import com.anishan.judge.domain.entity.LanguageConfig;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JudgeContent {
    private LanguageConfig languageConfig;
    private String testCasePath;
    private String testCaseContent;
    private Long maxTime;
    private Long maxMemory;
    private Long maxOutputSize;
    private Integer maxStack;
    private String fileId;
    private String fileContent;
    /**
     * fileIO 会导致stdout被覆盖，LocalFile也变成copy in了不知道做什么用的
     */
    private boolean fileIO;
    /**
     * 启用fileIO后这里变成
     * "copyIn": {
     *    "ioReadFileName": {
     *       "src": "testCasePath"
     *    }
     *},
     */
    private String ioReadFileName;

    /**
     * FileIO启用后
     * "copyOut": [
     *     "stderr",
     *     "ioWriteFileName?"
     * ]
     */
    private String ioWriteFileName;
}

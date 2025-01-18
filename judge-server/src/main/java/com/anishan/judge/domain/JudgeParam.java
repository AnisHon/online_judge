package com.anishan.judge.domain;

import com.anishan.judge.domain.entity.LanguageConfig;
import lombok.Builder;
import lombok.Data;

/**
 * 通用判题参数
 */
@Data
@Builder
public class JudgeParam {
    private LanguageConfig languageConfig;
    private String fileId;
    private String content;
    private boolean test;
}

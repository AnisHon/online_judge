package com.anishan.judge.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CaseContent {

    private BigDecimal score;

    /**
     * 输入OSS路径 -> 会映射为本地挂载路径
     */
    private String inPath;

    /**
     * 输出OSS路径 -> 会映射为本地挂载路径
     */
    private String outPath;

    private Long outSize;
}

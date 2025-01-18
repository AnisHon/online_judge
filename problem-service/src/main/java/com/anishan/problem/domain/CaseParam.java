package com.anishan.problem.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;

/**
 * 用于传递保存case参数的类
 */
@Data
@Accessors(chain = true)
public class CaseParam {

    private String in;
    private String out;
    private InputStream inCase;
    private InputStream outCase;

}

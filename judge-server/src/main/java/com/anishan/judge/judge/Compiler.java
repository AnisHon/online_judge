package com.anishan.judge.judge;

import com.anishan.judge.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;

import java.util.HashMap;

public interface Compiler {
    String compile(LanguageConfig languageConfig, String code,
                   String language, HashMap<String, String> extraFiles) throws SystemError, CompileError, SubmitError;

    Boolean compileSpj(String code, Long pid, String language, HashMap<String, String> extraFiles) throws SystemError;

    Boolean compileInteractive(String code, Long pid, String language, HashMap<String, String> extraFiles) throws SystemError;
}

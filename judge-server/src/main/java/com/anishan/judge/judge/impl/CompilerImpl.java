package com.anishan.judge.judge.impl;


import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.anishan.judge.config.LanguageConfigLoader;
import com.anishan.judge.domain.entity.LanguageConfig;
import com.anishan.judge.exception.CompileError;
import com.anishan.judge.exception.SubmitError;
import com.anishan.judge.exception.SystemError;
import com.anishan.judge.judge.Compiler;
import com.anishan.judge.util.Constants;
import com.anishan.judge.util.JudgeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CompilerImpl implements Compiler {

    private final SandboxRunImpl sandboxRun;

    @Override
    public String compile(LanguageConfig languageConfig, String code,
                          String language, HashMap<String, String> extraFiles) throws SystemError, CompileError, SubmitError {


        if (languageConfig == null) {
            throw new RuntimeException("Unsupported language " + language);
        }

        // 调用安全沙箱进行编译
        JSONArray result = sandboxRun.compile(languageConfig.getMaxCpuTime(),
                languageConfig.getMaxRealTime(),
                languageConfig.getMaxMemory(),
                256 * 1024 * 1024L,
                languageConfig.getSrcName(),
                languageConfig.getExeName(),
                parseCompileCommand(languageConfig.getCompileCommand()),
                languageConfig.getCompileEnvs(),
                code,
                extraFiles,
                true,
                false,
                null
        );
        JSONObject compileResult = (JSONObject) result.get(0);
        if (compileResult.getInt("status").intValue() != Constants.Judge.STATUS_ACCEPTED.getStatus()) {
            throw new CompileError("Compile Error.", ((JSONObject) compileResult.get("files")).getStr("stdout"),
                    ((JSONObject) compileResult.get("files")).getStr("stderr"));
        }

        String fileId = ((JSONObject) compileResult.get("fileIds")).getStr(languageConfig.getExeName());
        if (StrUtil.isEmpty(fileId)) {
            throw new SubmitError("Executable file not found.", ((JSONObject) compileResult.get("files")).getStr("stdout"),
                    ((JSONObject) compileResult.get("files")).getStr("stderr"));
        }
        return fileId;
    }

    @Override
    public Boolean compileSpj(String code, Long pid, String language, HashMap<String, String> extraFiles) throws SystemError {

        LanguageConfigLoader languageConfigLoader = SpringUtil.getBean(LanguageConfigLoader.class);
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName("SPJ-" + language);

        if (languageConfig == null) {
            throw new RuntimeException("Unsupported SPJ language:" + language);
        }

        boolean copyOutExe = pid != null;
        // 题目id为空，则不进行本地存储，可能为新建题目时测试特判程序是否正常的判断而已

        // 调用安全沙箱对特别判题程序进行编译
        JSONArray res = sandboxRun.compile(languageConfig.getMaxCpuTime(),
                languageConfig.getMaxRealTime(),
                languageConfig.getMaxMemory(),
                256 * 1024 * 1024L,
                languageConfig.getSrcName(),
                languageConfig.getExeName(),
                parseCompileCommand(languageConfig.getCompileCommand()),
                languageConfig.getCompileEnvs(),
                code,
                extraFiles,
                false,
                copyOutExe,
                Constants.JudgeDir.SPJ_WORKPLACE_DIR.getContent() + File.separator + pid
        );
        JSONObject compileResult = (JSONObject) res.get(0);
        if (compileResult.getInt("status").intValue() != Constants.Judge.STATUS_ACCEPTED.getStatus()) {
            throw new SystemError("Special Judge Code Compile Error.", ((JSONObject) compileResult.get("files")).getStr("stdout"),
                    ((JSONObject) compileResult.get("files")).getStr("stderr"));
        }
        return true;
    }

    @Override
    public Boolean compileInteractive(String code, Long pid, String language, HashMap<String, String> extraFiles) throws SystemError {

        LanguageConfigLoader languageConfigLoader = SpringUtil.getBean(LanguageConfigLoader.class);
        LanguageConfig languageConfig = languageConfigLoader.getLanguageConfigByName("INTERACTIVE-" + language);

        if (languageConfig == null) {
            throw new RuntimeException("Unsupported interactive language:" + language);
        }

        boolean copyOutExe = pid != null;
        // 题目id为空，则不进行本地存储，可能为新建题目时测试特判程序是否正常的判断而已

        // 调用安全沙箱对特别判题程序进行编译
        JSONArray res = sandboxRun.compile(languageConfig.getMaxCpuTime(),
                languageConfig.getMaxRealTime(),
                languageConfig.getMaxMemory(),
                256 * 1024 * 1024L,
                languageConfig.getSrcName(),
                languageConfig.getExeName(),
                parseCompileCommand(languageConfig.getCompileCommand()),
                languageConfig.getCompileEnvs(),
                code,
                extraFiles,
                false,
                copyOutExe,
                Constants.JudgeDir.INTERACTIVE_WORKPLACE_DIR.getContent() + File.separator + pid
        );
        JSONObject compileResult = (JSONObject) res.get(0);
        if (compileResult.getInt("status").intValue() != Constants.Judge.STATUS_ACCEPTED.getStatus()) {
            throw new SystemError("Interactive Judge Code Compile Error.", ((JSONObject) compileResult.get("files")).getStr("stdout"),
                    ((JSONObject) compileResult.get("files")).getStr("stderr"));
        }
        return true;
    }

    private static List<String> parseCompileCommand(String command) {
        return JudgeUtils.translateCommandline(command);
    }
}

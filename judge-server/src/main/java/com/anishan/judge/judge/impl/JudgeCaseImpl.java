package com.anishan.judge.judge.impl;

import cn.hutool.core.io.IoUtil;
import com.anishan.api.config.ConstConfig;
import com.anishan.commons.enumeration.CaseFileType;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.judge.judge.JudgeCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeCaseImpl implements JudgeCase {

    private final ConstConfig constConfig;


    private String getCasePath(Long problemId, Long caseId, CaseFileType fileType) {
        StringBuilder path = new StringBuilder();
        path
                .append(constConfig.getFileBaseUrl())
                .append(File.separator)
                .append(constConfig.getCasesFolder())
                .append(File.separator)
                .append(problemId)
                .append(File.separator)
                .append(caseId);
        switch (fileType) {
            case In:
                path.append(".in");
                break;
            case Out:
                path.append(".out");
                break;
            default:
                throw new UnsupportedOperationException("只能是in或者out");
        }
        return path.toString();
    }

    private String getProblemPath(Long problemId) {

        return constConfig.getFileBaseUrl() +
                File.separator +
                constConfig.getCasesFolder() +
                File.separator +
                problemId;


    }

    private void checkAndMakeFile(File file) {
        ThrowUtil.illegalState(file.isDirectory(), "该文件是文件夹");

        boolean fileAvailable = file.exists();
        boolean parentExists = file.getParentFile().exists();
        if (!parentExists) {
            if (file.getParentFile().mkdirs()) {
                log.error("严重错误，父文件夹无法创建{}", file.getAbsolutePath());
            }
        }
        if (!fileAvailable) {
            try {
                if (!file.createNewFile()) {
                    throw new IOException("无法创建文件");
                }
            } catch (IOException e) {
                log.error("严重错误，无法创建文件{}", file.getAbsoluteFile(),e);
                throw new RuntimeException(e);
            }
        }
    }

    private Reader getCaseReader(String path) {
        File file = new File(path);
        return getCaseReader(file);
    }

    private Reader getCaseReader(File file) {
        if (file == null) {
            return null;
        }

        checkAndMakeFile(file);
        BufferedReader reader;
        try {
            reader = IoUtil.getUtf8Reader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return reader;
    }

    private File getProblemFile(String path) {
        File file = new File(path);

        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("严重错误, 文件夹创建失败");
                throw new IllegalStateException("严重错误, 文件夹创建失败");
            }
        }

        return file;
    }

    private Writer getCaseWriter(String path) {
        File file = new File(path);
        checkAndMakeFile(file);
        OutputStreamWriter writer;
        try {
            writer = IoUtil.getUtf8Writer(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            log.error("严重错误，无法打开文件", e);
            throw new RuntimeException(e);
        }

        return writer;
    }

    /**
     * 获取所有case的file对象
     * @param path 路径
     * @param fileType 类型in out 或者all
     * @return 返回File数组
     */
    private File[] getCaseFiles(String path, CaseFileType fileType) {
        File problemFile = getProblemFile(path);
        return problemFile.listFiles((dir, name) -> {
            boolean valid = dir.isFile();
            switch (fileType) {
                case In:
                    valid = valid && name.endsWith(".in");
                    break;
                case Out:
                    valid = valid && name.endsWith(".out");
                    break;
            }
            return valid;
        });
    }

    private void writeFile(String path, String content) {
        try (Writer caseWriter = getCaseWriter(path)) {
            caseWriter.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeFile(String path, InputStream content) {
        try (Writer caseWriter = getCaseWriter(path)) {
            BufferedReader reader = IoUtil.getUtf8Reader(content);
            IoUtil.copy(reader, caseWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 写Case文件
     * @param problemId 问题ID
     * @param caseId    测试用例Id
     * @param input     测试用例输入
     * @param output    测试用例输出
     */
    @Override
    public void setCase(Long problemId, Long caseId, String input, String output) {
        String inputCasePath = getCasePath(problemId, caseId, CaseFileType.In);
        String outputCasePath = getCasePath(problemId, caseId, CaseFileType.Out);

        if (input!= null) {
            writeFile(inputCasePath, input);
        }
        if (output != null) {
            writeFile(outputCasePath, output);
        }
    }

    @Override
    public void setCase(Long problemId, Long caseId, InputStream input, InputStream output) {
        String inputCasePath = getCasePath(problemId, caseId, CaseFileType.In);
        String outputCasePath = getCasePath(problemId, caseId, CaseFileType.Out);

        if (input!= null) {
            writeFile(inputCasePath, input);
        }
        if (output != null) {
            writeFile(outputCasePath, output);
        }
    }

    @Override
    public List<Reader> getCases(Long problemId, CaseFileType fileType) {
        List<String> caseUrls = getCaseUrls(problemId, fileType);

        List<Reader> cases = new ArrayList<>();

        for (String caseUrl : caseUrls) {
            cases.add(getCaseReader(caseUrl));
        }

        return cases;
    }

    @Override
    public List<String> getCaseUrls(Long problemId, CaseFileType fileType) {
        String problemPath = getProblemPath(problemId);
        return Arrays
                .stream(getCaseFiles(problemPath, fileType))
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getCasesAsText(Long problemId) {
        return List.of();
    }

    @Override
    public void deleteCases(Long problemId, List<Long> caseIds) {
        if (caseIds == null) {
            return;
        }
        for (Long caseId : caseIds) {
            deleteCase(problemId, caseId);
        }


    }

    @Override
    public void deleteCase(Long problemId, Long caseId) {
        String inCase = getCasePath(problemId, caseId, CaseFileType.In);
        String outCase = getCasePath(problemId, caseId, CaseFileType.Out);

        new File(inCase).delete();
        new File(outCase).delete();
    }
}

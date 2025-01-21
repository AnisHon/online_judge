package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjUtil;
import com.anishan.api.client.content.domain.OSSFileInfo;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.api.client.judgeserver.domain.OjProblemCaseDto;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.api.file.FileOperation;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import com.anishan.problem.service.OjProblemCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class OjProblemCaseServiceImpl extends ServiceImpl<OjProblemCaseMapper, OjProblemCase>
    implements OjProblemCaseService {

    private final FileOperation fileOperation;

    public OjProblemCaseServiceImpl(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }

    /**
     * 获取Path
     * @param problemId 题目ID
     * @param caseId 测试用例Id
     * @param in 是输入还是输出（true->.in false->.out）
     * @return OSS路径
     */
    private static String getPath(Long problemId, Long caseId, boolean in) {
        return problemId + "/" + caseId + "." + (in ? "in" : "out");
    }

    @Override
    public LocalDateTime selectTime(Long id) {
        return this.getObj(new LambdaQueryWrapper<OjProblemCase>()
                .select(OjProblemCase::getUpdateTime)
                .eq(OjProblemCase::getCaseId, id),
                x -> (LocalDateTime) x
        );
    }

    @Override
    public List<OjProblemCaseVo> getByProblemId(Long problemId) {
        List<OjProblemCase> list = list(new LambdaQueryWrapper<OjProblemCase>()
                .eq(OjProblemCase::getProblemId, problemId)
        );


        return BeanUtil.copyToList(list, OjProblemCaseVo.class);
    }

    @Override
    public List<OjProblemCaseVo> getCaseVoById(Long problemId) {
        List<OjProblemCase> list = this.list(
                Wrappers.lambdaQuery(OjProblemCase.class)
                        .eq(OjProblemCase::getProblemId, problemId)
        );


        List<OjProblemCaseVo> cases = BeanUtil.copyToList(list, OjProblemCaseVo.class);
        cases
                .forEach(ojProblemCaseVo -> {
                    String input = ojProblemCaseVo.getInput();
                    String output = ojProblemCaseVo.getOutput();
                    OSSFileInfo in = fileOperation.getFileInfo(input);
                    OSSFileInfo out = fileOperation.getFileInfo(output);

                    ojProblemCaseVo.setInputSize(in.getSize());
                    ojProblemCaseVo.setOutputSize(out.getSize());
                });
        return cases;
    }

    @Override
    @Transactional
    @SneakyThrows
    public boolean addOjProblemCase(OjProblemCaseDto ojProblemCase) {

        String inputText = Optional.ofNullable(ojProblemCase.getInput()).orElse("");
        String outputText = Optional.ofNullable(ojProblemCase.getOutput()).orElse("");
        MultipartFile inputFile = ojProblemCase.getInputFile();
        MultipartFile outputFile = ojProblemCase.getOutputFile();

        long id = IdWorker.getId();


        OjProblemCase problemCase = saveAndBuildCase(ojProblemCase, inputText, outputText, inputFile, outputFile, id);

        return this.save(problemCase);
    }

    private OjProblemCase saveAndBuildCase(OjProblemCaseDto ojProblemCase, String inputText, String outputText, MultipartFile inputFile, MultipartFile outputFile, long id) throws IOException {
        String inPath = getPath(ojProblemCase.getProblemId(), id, true);
        String outPath = getPath(ojProblemCase.getProblemId(), id, false);

        ojProblemCase.setInput(inPath);
        ojProblemCase.setOutput(outPath);

        OjProblemCase problemCase =
                BeanUtil.copyProperties(ojProblemCase,
                                OjProblemCase.class, "inputFile", "outputFile")
                        .setCaseId(id);

        if (ObjUtil.isNotNull(inputFile)) {
            // 上传 input
            fileOperation.saveFile(inPath, inputFile.getInputStream());
       } else {
           ByteArrayInputStream inIs = new ByteArrayInputStream(inputText.getBytes(StandardCharsets.UTF_8));
           fileOperation.saveFile(inPath, inIs);
       }

        if (ObjUtil.isNotNull(outputFile)) {
            // 上传 output
            fileOperation.saveFile(outPath, outputFile.getInputStream());
       } else {
           // 上传 output
           ByteArrayInputStream outIs = new ByteArrayInputStream(outputText.getBytes(StandardCharsets.UTF_8));
           fileOperation.saveFile(outPath, outIs);
       }
        return problemCase;
    }

    @Override
    @Transactional
    public boolean removeCase(List<Long> caseId) {
        List<OjProblemCase> cases = this.listByIds(caseId);




        boolean b = this.removeByIds(caseId);

        cases.forEach(problemCase -> {
            String input = problemCase.getInput();
            String output = problemCase.getOutput();
            fileOperation.deleteFile(input);
            fileOperation.deleteFile(output);
        });

        return b;
    }

    @Override
    public void download(String path, HttpServletResponse response) {
        try {
            OssFileInputStream file = fileOperation.getFile(path);
            response.setContentType("text/plain;charset=utf-8");
            ServletOutputStream os = response.getOutputStream();
            IoUtil.copy(file, os);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}





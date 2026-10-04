package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import com.anishan.api.client.content.domain.OSSFileInfo;
import com.anishan.api.client.content.domain.OssFileInputStream;
import com.anishan.api.client.judgeserver.domain.OjProblemCaseDto;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.api.file.FileOperation;
import com.anishan.commons.exception.BusinessException;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import com.anishan.problem.service.ContestMutationGuard;
import com.anishan.problem.service.OjProblemCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@Slf4j
public class OjProblemCaseServiceImpl extends ServiceImpl<OjProblemCaseMapper, OjProblemCase>
    implements OjProblemCaseService {

    public static final long MAX_INLINE_CASE_BYTES = 1024L * 1024L;

    private final FileOperation fileOperation;
    private final ContestMutationGuard mutationGuard;
    private final TransactionTemplate transactionTemplate;

    public OjProblemCaseServiceImpl(FileOperation fileOperation,
                                    ContestMutationGuard mutationGuard,
                                    PlatformTransactionManager transactionManager) {
        this.fileOperation = fileOperation;
        this.mutationGuard = mutationGuard;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
    public String readCaseContent(Long caseId, String field) {
        OjProblemCase problemCase = getById(caseId);
        ThrowUtil.businessError(problemCase == null, "测试用例不存在");

        String path = casePath(problemCase, field);
        OSSFileInfo fileInfo = fileOperation.getFileInfo(path);
        ThrowUtil.businessError(fileInfo == null, "测试用例文件不存在");
        ThrowUtil.businessError(fileInfo.getSize() > MAX_INLINE_CASE_BYTES,
                "测试用例超过1MiB，不能在线查看，请下载后本地编辑并上传替换");

        try (OssFileInputStream stream = fileOperation.getFile(path)) {
            ThrowUtil.businessError(stream == null, "测试用例文件读取失败");
            byte[] content = stream.readNBytes((int) MAX_INLINE_CASE_BYTES + 1);
            ThrowUtil.businessError(content.length > MAX_INLINE_CASE_BYTES,
                    "测试用例超过1MiB，不能在线查看，请下载后本地编辑并上传替换");
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("测试用例读取失败");
        }
    }

    @Override
    public boolean updateOjProblemCase(Long caseId, OjProblemCaseDto dto) {
        OjProblemCase current = getById(caseId);
        ThrowUtil.businessError(current == null, "测试用例不存在");
        ThrowUtil.businessError(dto == null, "测试用例更新内容不能为空");

        byte[] inputText = textBytes(dto.getInput());
        byte[] outputText = textBytes(dto.getOutput());
        validateInlineSize(inputText);
        validateInlineSize(outputText);

        boolean hasChanges = dto.getScore() != null
                || dto.getInputFile() != null
                || dto.getOutputFile() != null
                || inputText != null
                || outputText != null;
        ThrowUtil.businessError(!hasChanges, "请至少修改分数或测试用例文件");
        if (dto.getScore() != null && dto.getScore().signum() < 0) {
            throw new BusinessException("测试用例分数不能为负数");
        }

        StagedCaseFiles staged = stageReplacementFiles(current, dto, inputText, outputText);
        boolean committed = false;
        try {
            Boolean result = transactionTemplate.execute(status -> {
                ContestMutationGuard.MutationContext context = mutationGuard.lockAndInspect(
                        Collections.singletonList(current.getProblemId()));
                mutationGuard.requireScoringMutable(context);
                OjProblemCase lockedCase = findLockedCase(context.getLockedCases(), caseId);
                if (lockedCase == null || !current.getProblemId().equals(lockedCase.getProblemId())) {
                    throw new BusinessException("测试用例已被删除或修改，请刷新后重试");
                }

                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OjProblemCase> update =
                        Wrappers.lambdaUpdate(OjProblemCase.class)
                                .eq(OjProblemCase::getCaseId, caseId)
                                .eq(OjProblemCase::getProblemId, current.getProblemId());
                boolean changed = false;
                if (staged.inputPath != null) {
                    update.set(OjProblemCase::getInput, staged.inputPath);
                    changed = true;
                }
                if (staged.outputPath != null) {
                    update.set(OjProblemCase::getOutput, staged.outputPath);
                    changed = true;
                }
                if (dto.getScore() != null) {
                    update.set(OjProblemCase::getScore, dto.getScore());
                    changed = true;
                }
                if (!changed) return true;
                int rows = baseMapper.update(null, update);
                if (rows == 0 && staged.hasUploadedFiles()) {
                    throw new BusinessException("测试用例引用更新失败，请刷新后重试");
                }
                return true;
            });
            committed = Boolean.TRUE.equals(result);
            return committed;
        } finally {
            if (!committed) cleanupStagedFiles(staged.uploadedPaths);
        }
    }

    private static byte[] textBytes(String text) {
        return text == null ? null : text.getBytes(StandardCharsets.UTF_8);
    }

    private static void validateInlineSize(byte[] content) {
        ThrowUtil.businessError(content != null && content.length > MAX_INLINE_CASE_BYTES,
                "单个测试用例在线编辑不能超过1MiB，请使用文件上传");
    }

    private static String casePath(OjProblemCase problemCase, String field) {
        String normalized = field == null ? "" : field.toLowerCase(Locale.ROOT);
        if ("input".equals(normalized)) return problemCase.getInput();
        if ("output".equals(normalized)) return problemCase.getOutput();
        throw new BusinessException("测试用例字段无效");
    }

    @Override
    public boolean addOjProblemCase(OjProblemCaseDto ojProblemCase) {
        if (ojProblemCase == null || ojProblemCase.getProblemId() == null || ojProblemCase.getProblemId() <= 0) {
            throw new BusinessException("题目ID无效");
        }
        String inputText = Optional.ofNullable(ojProblemCase.getInput()).orElse("");
        String outputText = Optional.ofNullable(ojProblemCase.getOutput()).orElse("");
        long id = IdWorker.getId();
        CaseFiles files = stageNewCaseFiles(ojProblemCase.getProblemId(), id,
                ojProblemCase.getInputFile(), ojProblemCase.getOutputFile(), inputText, outputText);
        OjProblemCase problemCase = BeanUtil.copyProperties(ojProblemCase,
                OjProblemCase.class, "inputFile", "outputFile");
        problemCase.setCaseId(id).setInput(files.inputPath).setOutput(files.outputPath);

        boolean committed = false;
        try {
            Boolean result = transactionTemplate.execute(status -> {
                ContestMutationGuard.MutationContext context = mutationGuard.lockAndInspect(
                        Collections.singletonList(problemCase.getProblemId()));
                mutationGuard.requireScoringMutable(context);
                if (!save(problemCase)) {
                    throw new BusinessException("测试用例保存失败");
                }
                return true;
            });
            committed = Boolean.TRUE.equals(result);
            return committed;
        } finally {
            if (!committed) cleanupStagedFiles(files.uploadedPaths);
        }
    }

    @Override
    public boolean removeCase(List<Long> caseId) {
        List<Long> ids = normalizeCaseIds(caseId);
        if (ids.isEmpty()) return false;
        List<OjProblemCase> existing = this.listByIds(ids);
        if (existing.isEmpty()) return false;
        List<Long> problemIds = existing.stream().map(OjProblemCase::getProblemId)
                .distinct().sorted().collect(Collectors.toList());

        Boolean result = transactionTemplate.execute(status -> {
            ContestMutationGuard.MutationContext context = mutationGuard.lockAndInspect(problemIds);
            mutationGuard.requireScoringMutable(context);
            Set<Long> lockedCaseIds = context.getLockedCases().stream()
                    .map(OjProblemCase::getCaseId).collect(Collectors.toSet());
            List<Long> deletableIds = ids.stream().filter(lockedCaseIds::contains).collect(Collectors.toList());
            return !deletableIds.isEmpty() && this.removeByIds(deletableIds);
        });
        // Keep old objects: an already accepted practice judge may still be reading their paths.
        return Boolean.TRUE.equals(result);
    }

    private StagedCaseFiles stageReplacementFiles(OjProblemCase current, OjProblemCaseDto dto,
                                                   byte[] inputText, byte[] outputText) {
        StagedCaseFiles staged = new StagedCaseFiles();
        try {
            if (dto.getInputFile() != null || inputText != null) {
                staged.inputPath = stageOne(current.getProblemId(), current.getCaseId(), true,
                        dto.getInputFile(), inputText, staged.uploadedPaths);
            }
            if (dto.getOutputFile() != null || outputText != null) {
                staged.outputPath = stageOne(current.getProblemId(), current.getCaseId(), false,
                        dto.getOutputFile(), outputText, staged.uploadedPaths);
            }
            return staged;
        } catch (IOException | RuntimeException e) {
            cleanupStagedFiles(staged.uploadedPaths);
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException("测试用例文件更新失败");
        }
    }

    private CaseFiles stageNewCaseFiles(Long problemId, Long caseId,
                                        MultipartFile inputFile, MultipartFile outputFile,
                                        String inputText, String outputText) {
        CaseFiles files = new CaseFiles();
        String uuid = UUID.randomUUID().toString();
        files.inputPath = problemId + "/" + caseId + "-" + uuid + ".in";
        files.outputPath = problemId + "/" + caseId + "-" + uuid + ".out";
        try {
            stagePath(files.inputPath, inputFile, inputText.getBytes(StandardCharsets.UTF_8), files.uploadedPaths);
            stagePath(files.outputPath, outputFile, outputText.getBytes(StandardCharsets.UTF_8), files.uploadedPaths);
            return files;
        } catch (IOException | RuntimeException e) {
            cleanupStagedFiles(files.uploadedPaths);
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException("测试用例文件上传失败");
        }
    }

    private String stageOne(Long problemId, Long caseId, boolean input,
                            MultipartFile file, byte[] text, List<String> uploadedPaths) throws IOException {
        String path = problemId + "/" + caseId + "-" + UUID.randomUUID()
                + (input ? ".in" : ".out");
        stagePath(path, file, text, uploadedPaths);
        return path;
    }

    private void stagePath(String path, MultipartFile file, byte[] text, List<String> uploadedPaths)
            throws IOException {
        uploadedPaths.add(path);
        if (file != null) {
            try (java.io.InputStream stream = file.getInputStream()) {
                fileOperation.saveFile(path, stream, "text/plain");
            }
        } else {
            try (ByteArrayInputStream stream = new ByteArrayInputStream(text == null ? new byte[0] : text)) {
                fileOperation.saveFile(path, stream, "text/plain");
            }
        }
    }

    private OjProblemCase findLockedCase(List<OjProblemCase> cases, Long caseId) {
        if (cases == null) return null;
        return cases.stream().filter(item -> caseId.equals(item.getCaseId())).findFirst().orElse(null);
    }

    private List<Long> normalizeCaseIds(Collection<Long> values) {
        if (values == null || values.isEmpty()) return Collections.emptyList();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Long id : values) {
            if (id == null || id <= 0) throw new BusinessException("测试用例ID无效");
            ids.add(id);
        }
        return new ArrayList<>(ids);
    }

    private void cleanupStagedFiles(List<String> paths) {
        for (String path : paths) {
            try {
                fileOperation.deleteFile(path);
            } catch (RuntimeException cleanupError) {
                log.warn("Failed to clean staged case object after rollback; manual case-file reconciliation is required");
            }
        }
    }

    private static class CaseFiles {
        String inputPath;
        String outputPath;
        final List<String> uploadedPaths = new ArrayList<>();
    }

    private static final class StagedCaseFiles extends CaseFiles {
        private boolean hasUploadedFiles() {
            return !uploadedPaths.isEmpty();
        }
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

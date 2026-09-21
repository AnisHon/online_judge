package com.anishan.problem.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.commons.domain.R;
import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.anishan.problem.domain.dto.AdminJudgeLogQuery;
import com.anishan.problem.domain.vo.AdminJudgeCaseLogVo;
import com.anishan.problem.domain.vo.AdminSubmitLogVo;
import com.anishan.problem.service.JudgeCaseLogService;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.api.client.problem.domain.vo.SubmitCaseResultVo;
import com.anishan.problem.service.SubmitLogService;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.JudgeResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Api("内部接口，记录提交的")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogController {

    private final SubmitLogService submitLogService;
    private final JudgeCaseLogService judgeCaseLogService;

    @PostMapping("/queue")
    public R<Long> logQueue(JudgeMessage judgeMessage) {
        Long id = this.submitLogService.logQueue(
                judgeMessage.getUserId(),
                judgeMessage.getProblemId(),
                judgeMessage.getLanguage()
        );
        return R.success(id);
    }

    @PostMapping("/log-judge")
    public R<Long> logJudge(@RequestBody SubmitLogDto submitLog) {
        Long id = this.submitLogService.logJudge(submitLog);
        return R.success(id);
    }

    @PostMapping("/update")
    public R<Boolean> update(@RequestBody SubmitLogDto submitLog) {
        boolean b = this.submitLogService.update(submitLog);
        return R.success(b);
    }

    @PostMapping("/change-status")
    public R<Boolean> changeStatus(@RequestBody SubmitLogDto submitLog, @RequestHeader("user-id") Long userId) {
        SubmitLog log = BeanUtil.copyProperties(submitLog, SubmitLog.class);
        boolean b = submitLogService.changeStatus(userId, log.getSubmitId(), log.getStatus());
        return R.success(b);
    }

    @GetMapping("/get/{id}")
    @ApiOperation("外部接口，用户获取运行结果")
    @PreAuthorize("isAuthenticated()")
    public R<SubmitLogVo> getLog(@PathVariable("id") Long id, @RequestHeader("user-id")Long userId) {
        SubmitLogVo log = submitLogService.getLog(id, userId);
        return R.success(log);
    }

    /**
     * HTTP 短轮询接口。前端按 1 秒左右的频率调用即可。
     */
    @GetMapping("/submissions/{id}")
    @ApiOperation("轮询指定提交状态")
    @PreAuthorize("isAuthenticated()")
    public R<SubmitLogVo> poll(@PathVariable("id") Long id, @RequestHeader("user-id") Long userId) {
        return R.success(submitLogService.getLog(id, userId));
    }

    /**
     * 普通用户查看自己提交的测试点结果。
     *
     * <p>这是脱敏后的结果，只用于解释通过情况，不返回判题机内部错误、测试数据或 caseId。</p>
     */
    @GetMapping("/submissions/{id}/cases")
    @ApiOperation("查看自己的提交测试点结果")
    @PreAuthorize("isAuthenticated()")
    public R<List<SubmitCaseResultVo>> userCaseResults(
            @PathVariable("id") Long id,
            @RequestHeader("user-id") Long userId
    ) {
        // 先做归属校验。不存在或不属于当前用户时返回空列表，不泄露提交是否存在。
        if (submitLogService.getLog(id, userId) == null) {
            return R.success(java.util.Collections.emptyList());
        }
        List<SubmitCaseResultVo> results = judgeCaseLogService.list(new LambdaQueryWrapper<JudgeCaseLog>()
                        .eq(JudgeCaseLog::getSubmitId, id)
                        .orderByAsc(JudgeCaseLog::getCaseIndex))
                .stream()
                .map(item -> new SubmitCaseResultVo()
                        .setCaseIndex(item.getCaseIndex() == null ? null : item.getCaseIndex() + 1)
                        .setStatus(item.getStatus())
                        .setScore(item.getScore())
                        .setTime(item.getTime())
                        .setMemory(item.getMemory()))
                .collect(Collectors.toList());
        return R.success(results);
    }

    /** 管理员预留接口：查看每个测试用例和判题机内部错误。 */
    @GetMapping("/admin/{submitId}/cases")
    @ApiOperation("查看提交的内部测试用例日志")
    @PreAuthorize("hasAuthority('problem:judge:case:read')")
    public R<List<AdminJudgeCaseLogVo>> caseLogs(@PathVariable Long submitId) {
        return R.success(judgeCaseLogService.list(new LambdaQueryWrapper<JudgeCaseLog>()
                .eq(JudgeCaseLog::getSubmitId, submitId)
                .orderByAsc(JudgeCaseLog::getCaseIndex))
                .stream()
                .map(AdminJudgeCaseLogVo::from)
                .collect(Collectors.toList()));
    }

    /** 管理员预留接口：查看提交级内部错误（例如无测试用例、投递失败、沙箱异常）。 */
    @GetMapping("/admin/{submitId}")
    @ApiOperation("查看提交内部诊断信息")
    @PreAuthorize("hasAuthority('problem:judge:submit:read')")
    public R<AdminSubmitLogVo> adminDetail(@PathVariable Long submitId) {
        return R.success(AdminSubmitLogVo.from(submitLogService.getById(submitId)));
    }

    /** 管理侧查看用户一次完整提交的分页记录。 */
    @GetMapping("/admin/submissions")
    @ApiOperation("管理员分页查看判题提交记录")
    @PreAuthorize("hasAuthority('problem:judge:submit:read')")
    public R<PagedResult<AdminSubmitLogVo>> adminSubmissions(@Validated AdminJudgeLogQuery query) {
        LambdaQueryWrapper<SubmitLog> wrapper = new LambdaQueryWrapper<SubmitLog>()
                .eq(query.getSubmitId() != null, SubmitLog::getSubmitId, query.getSubmitId())
                .eq(query.getUserId() != null, SubmitLog::getUserId, query.getUserId())
                .eq(query.getProblemId() != null, SubmitLog::getProblemId, query.getProblemId())
                .eq(query.getContestId() != null, SubmitLog::getContestId, query.getContestId())
                .eq(org.springframework.util.StringUtils.hasText(query.getLanguage()), SubmitLog::getLanguage, query.getLanguage())
                .orderByDesc(SubmitLog::getSubmitTime);
        JudgeResult status = parseStatus(query.getStatus());
        if (org.springframework.util.StringUtils.hasText(query.getStatus()) && status == null) {
            return R.badRequest("不支持的判题状态");
        }
        wrapper.eq(status != null, SubmitLog::getStatus, status);
        Page<SubmitLog> page = new Page<>(query.getCurrentPage(), query.getPageSize());
        submitLogService.page(page, wrapper);
        List<AdminSubmitLogVo> records = page.getRecords().stream()
                .map(AdminSubmitLogVo::from)
                .collect(Collectors.toList());
        return PagedResult.<AdminSubmitLogVo, SubmitLog>fromPage(page, records, page.getTotal()).toR();
    }

    /** 管理侧查看逐测试用例的内部判题日志。 */
    @GetMapping("/admin/cases")
    @ApiOperation("管理员分页查看判题测试用例日志")
    @PreAuthorize("hasAuthority('problem:judge:case:read')")
    public R<PagedResult<AdminJudgeCaseLogVo>> adminCaseLogs(@Validated AdminJudgeLogQuery query) {
        LambdaQueryWrapper<JudgeCaseLog> wrapper = new LambdaQueryWrapper<JudgeCaseLog>()
                .eq(query.getSubmitId() != null, JudgeCaseLog::getSubmitId, query.getSubmitId())
                .eq(query.getProblemId() != null, JudgeCaseLog::getProblemId, query.getProblemId())
                .eq(query.getCaseId() != null, JudgeCaseLog::getCaseId, query.getCaseId())
                .eq(query.getCaseIndex() != null, JudgeCaseLog::getCaseIndex, query.getCaseIndex())
                .orderByDesc(JudgeCaseLog::getCreateTime)
                .orderByAsc(JudgeCaseLog::getCaseIndex);
        JudgeResult status = parseStatus(query.getStatus());
        if (org.springframework.util.StringUtils.hasText(query.getStatus()) && status == null) {
            return R.badRequest("不支持的判题状态");
        }
        wrapper.eq(status != null, JudgeCaseLog::getStatus, status);
        Page<JudgeCaseLog> page = new Page<>(query.getCurrentPage(), query.getPageSize());
        judgeCaseLogService.page(page, wrapper);
        List<AdminJudgeCaseLogVo> records = page.getRecords().stream()
                .map(AdminJudgeCaseLogVo::from)
                .collect(Collectors.toList());
        return PagedResult.<AdminJudgeCaseLogVo, JudgeCaseLog>fromPage(page, records, page.getTotal()).toR();
    }

    private JudgeResult parseStatus(String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (JudgeResult result : JudgeResult.values()) {
            if (result.name().equals(normalized) || result.getValue().equalsIgnoreCase(normalized)) {
                return result;
            }
        }
        return null;
    }

    @GetMapping("/recentSubmit/{problemId}")
    @ApiOperation("最近提交记录")
    @PreAuthorize("isAuthenticated()")
    public R<List<SubmitLogVo>> recentSubmit(
            @NotNull @PathVariable("problemId") Long problemId,
            @RequestHeader("user-id") Long userId
    ) {


        Page<SubmitLog> page = Page.of(1, 15);

        List<SubmitLog> list = submitLogService.list(page, new LambdaQueryWrapper<SubmitLog>()
                .eq(SubmitLog::getUserId, userId)
                .eq(SubmitLog::getProblemId, problemId)
                .orderByDesc(SubmitLog::getSubmitTime)
        );
        List<SubmitLogVo> submitLogVos = BeanUtil.copyToList(list, SubmitLogVo.class);
        return R.success(submitLogVos);
    }





}

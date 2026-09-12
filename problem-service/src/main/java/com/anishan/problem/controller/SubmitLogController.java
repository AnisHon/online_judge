package com.anishan.problem.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.commons.domain.R;
import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.anishan.problem.service.JudgeCaseLogService;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.problem.service.SubmitLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.NotNull;
import java.util.List;

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

    /** 管理员预留接口：查看每个测试用例和判题机内部错误。 */
    @GetMapping("/admin/{submitId}/cases")
    @ApiOperation("查看提交的内部测试用例日志")
    @PreAuthorize("hasAuthority('problem:judge:case:read')")
    public R<List<JudgeCaseLog>> caseLogs(@PathVariable Long submitId) {
        return R.success(judgeCaseLogService.list(new LambdaQueryWrapper<JudgeCaseLog>()
                .eq(JudgeCaseLog::getSubmitId, submitId)
                .orderByAsc(JudgeCaseLog::getCaseIndex)));
    }

    /** 管理员预留接口：查看提交级内部错误（例如无测试用例、投递失败、沙箱异常）。 */
    @GetMapping("/admin/{submitId}")
    @ApiOperation("查看提交内部诊断信息")
    @PreAuthorize("hasAuthority('problem:judge:case:read')")
    public R<SubmitLog> adminDetail(@PathVariable Long submitId) {
        return R.success(submitLogService.getById(submitId));
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

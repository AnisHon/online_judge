package com.anishan.problem.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.commons.domain.R;
import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.problem.service.SubmitLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Api("内部接口，记录提交的")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogController {

    private final SubmitLogService submitLogService;

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
    public R<SubmitLogVo> getLog(@PathVariable("id") Long id, @RequestHeader("user-id")Long userId) {
        SubmitLogVo log = submitLogService.getLog(id, userId);
        return R.success(log);
    }

    @GetMapping("/recentSubmit/{problemId}")
    @ApiOperation("最近提交记录")
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

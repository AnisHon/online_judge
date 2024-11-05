package com.anishan.problem.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.commons.domain.R;
import com.anishan.problem.domain.dto.SubmitLogDto;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.domain.vo.SubmitLogVo;
import com.anishan.problem.service.SubmitLogService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api("内部接口，记录提交的")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogController {

    private final SubmitLogService submitLogService;

    @PostMapping("compiling")
    public R<Long> logCompiling(JudgeMessage judgeMessage) {
        Long id = this.submitLogService.logCompiling(
                judgeMessage.getUserId(),
                judgeMessage.getProblemId(),
                judgeMessage.getLanguage()
        );
        return R.success(id);
    }

    @PostMapping("log-judge")
    public R<Long> logJudge(@RequestBody SubmitLog submitLog) {
        Long id = this.submitLogService.logJudge(submitLog);
        return R.success(id);
    }

    @PostMapping("change-status")
    public R<Boolean> changeStatus(@RequestBody SubmitLogDto submitLog) {
        SubmitLog log = BeanUtil.copyProperties(submitLog, SubmitLog.class);
        boolean b = submitLogService.changeStatus(log.getSubmitId(), log.getStatus());
        return R.success(b);
    }

    @GetMapping("/get/{id}")
    public R<SubmitLogVo> getLog(@PathVariable("id") Long id) {
        SubmitLogVo log = submitLogService.getLog(id);
        return R.success(log);
    }



}

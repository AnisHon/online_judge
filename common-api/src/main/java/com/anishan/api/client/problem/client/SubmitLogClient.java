package com.anishan.api.client.problem.client;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeMessage;
import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "problem-service", contextId = "submit-log", path = "log", configuration = FeignDecoderConfig.class)
public interface SubmitLogClient {
    @PostMapping("compiling")
    R<Long> logCompiling(JudgeMessage judgeMessage, @RequestHeader("user-id") Long userId);

    @PostMapping("log-judge")
    R<Long> logJudge(@RequestBody SubmitLogDto submitLog, @RequestHeader("user-id") Long userId);

    @PostMapping("change-status")
    R<Boolean> changeStatus(@RequestBody SubmitLogDto submitLog, @RequestHeader("user-id") Long userId);

    @GetMapping("/get/{id}")
    R<SubmitLogVo> getLog(@PathVariable("id") Long id, @RequestHeader("user-id") Long userId);

    @GetMapping("/submissions/{id}")
    R<SubmitLogVo> poll(@PathVariable("id") Long id, @RequestHeader("user-id") Long userId);

    @PostMapping("update")
    R<Boolean> update(@RequestBody SubmitLogDto submitLog, @RequestHeader("user-id") Long userId);
}


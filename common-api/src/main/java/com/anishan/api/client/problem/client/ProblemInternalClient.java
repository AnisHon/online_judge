package com.anishan.api.client.problem.client;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "problem-service", contextId = "problem-internal", path = "internal", configuration = FeignDecoderConfig.class)
public interface ProblemInternalClient {

    @ApiOperation("存储判题结果用的")
    @PostMapping("/judgeResult")
    R<Void> judgeResult(@RequestBody JudgeScore judgeScore);

    @ApiOperation("更新判题公开状态")
    @PostMapping("/judgeStatus")
    R<Void> judgeStatus(@RequestBody JudgeScore judgeScore);
}

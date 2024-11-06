package com.anishan.api.client.problem.client;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "problem", contextId = "record")
public interface RecordClient {

    @PostMapping("judge-save")
    @ApiOperation("内部接口，保存judge数据")
    R<Void> judgeSave(@Header("user-id") Long userId, @RequestBody JudgeScore judgeScore);
}

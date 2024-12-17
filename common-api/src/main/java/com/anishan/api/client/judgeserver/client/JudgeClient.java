package com.anishan.api.client.judgeserver.client;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.OjProblemCaseDto;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;


@FeignClient(value = "judge-server", contextId = "judge", path = "internal", configuration = FeignDecoderConfig.class)
public interface JudgeClient {

    @PostMapping(value = "judge-save")
    void judgeSave(@RequestBody JudgeScore judgeScore, @RequestHeader("user-id") Long userId);

    @ApiOperation("判题接口")
    @PostMapping("/judge")
    R<Boolean> judge(@RequestBody JudgeInfo judgeInfo);

    @ApiOperation("测试代码接口")
    @PostMapping("/test")
    R<Boolean> test(@RequestBody RunTestInfo runTestInfo);

    @ApiOperation("设置Case接口")
    @PostMapping("/set-case/{problemId}")
    R<Void> setCase(@PathVariable("problemId") Long problemId, @RequestBody List<OjProblemCase> cases);


}

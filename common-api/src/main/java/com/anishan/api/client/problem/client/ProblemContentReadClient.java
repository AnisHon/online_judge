package com.anishan.api.client.problem.client;

import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.config.FeignDecoderConfig;
import com.anishan.commons.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        value = "problem-service",
        contextId = "problem-content-read",
        path = "/internal",
        configuration = FeignDecoderConfig.class
)
public interface ProblemContentReadClient {

    @PostMapping("/content-problems/read")
    R<List<ContentProblemReadVo>> readProblemContent(@RequestBody ContentProblemReadRequest request);
}

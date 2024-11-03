package com.anishan.api.client.gojudge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "http://localhost:5050", url = "http://localhost:5050")
public interface GoJudgeClient {

    @GetMapping("/version")
    JsonNode version();

    @PostMapping("/run")
    JsonNode run(@RequestBody JsonNode cmd);


}

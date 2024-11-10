package com.anishan.api.client.gojudge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.anishan.api.client.gojudge.domain.GoJudgeVersion;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "judge-service", url = "${judge.url}")
public interface GoJudgeClient {

    @GetMapping("/version")
    GoJudgeVersion version();

    @PostMapping("/run")
    JSONArray run(@RequestBody JSONObject cmd);

    @DeleteMapping("/file/{id}")
    void delete(@PathVariable("id") String id);

}

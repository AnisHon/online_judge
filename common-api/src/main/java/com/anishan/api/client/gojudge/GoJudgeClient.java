package com.anishan.api.client.gojudge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "http://8.149.133.76:5050", url = "http://8.149.133.76:5050")
public interface GoJudgeClient {

    @GetMapping("/version")
    JsonNode version();

    @PostMapping("/run")
    JSONArray run(@RequestBody JSONObject cmd);

    @DeleteMapping("/file/{id}")
    void delete(@PathVariable("id") String id);

}

package com.anishan.content.controller;

import com.anishan.commons.domain.R;
import com.anishan.content.service.SseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/info")
@Api("信息接口")
@RequiredArgsConstructor
public class InfoController {

    private final SseService sseService;

    @Value("${site.name:言语代码}")
    private String siteName;

    @Value("${site.icp-number:津ICP备2025028121号-1}")
    private String icpNumber;

    @ApiOperation("站点配置")
    @GetMapping("/config")
    public R<Map<String, String>> config() {
        Map<String, String> data = new HashMap<>();
        data.put("siteName", siteName);
        data.put("icpNumber", icpNumber);
        return R.success(data);
    }

    @ResponseBody
    @ApiOperation("在线人数")
    @GetMapping("/online")
//    @PreAuthorize("hasAuthority('content:info')")
    public R<Long> online() {
        return R.success(sseService.count());
    }

    @ResponseBody
    @ApiOperation("剩余空间/总内存 free total")
    @GetMapping("/free")
//    @PreAuthorize("hasAuthority('content:info')")
    public R<HashMap<String, Integer>> disk() {
        HashMap<String, Integer> data = new HashMap<>();

        data.put("free", 100);
        data.put("total", 500);
        return R.success(data);
    }


}

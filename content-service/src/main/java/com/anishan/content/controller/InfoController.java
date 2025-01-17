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

import java.util.Map;

@RestController
@RequestMapping("/info")
@Api("信息接口")
@RequiredArgsConstructor
public class InfoController {

    private final SseService sseService;

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
    public R<Map<String, Long>> free() {
        R<Map<String, Long>> result = R.withMap();
        Map<String, Long> data = result.getData();

        data.put("free", 100L);
        data.put("total", 500L);

        return result;
    }



}

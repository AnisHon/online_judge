package com.anishan.content.controller;

import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/info")
@RequiredArgsConstructor
public class InfoController {

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

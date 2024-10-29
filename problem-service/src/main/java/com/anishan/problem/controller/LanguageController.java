package com.anishan.problem.controller;

import com.anishan.commons.domain.R;
import com.anishan.problem.domain.vo.SysLanguageVo;
import com.anishan.problem.service.SysLanguageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/language")
@Api("编程语言接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LanguageController {

    private final SysLanguageService sysLanguageService;

    @GetMapping("/list")
    @ApiOperation("列出所有编程语言")
    public R<List<SysLanguageVo>> list() {
        List<SysLanguageVo> sysLanguageVos = sysLanguageService.listAll();
        return R.success(sysLanguageVos);
    }



}

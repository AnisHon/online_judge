package com.anishan.user.controller;


import com.anishan.commons.entity.R;
import com.anishan.user.entity.vo.TreedMenuVo;
import com.anishan.user.service.SysMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api("认证相关接口")
@RestController("/auth")
public class AuthenticationController {


    SysMenuService sysMenuService;

    @Autowired
    public AuthenticationController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    @GetMapping("/menus")
    @ApiOperation("获取所有菜单，以树状的形式返回")
    public R<List<TreedMenuVo>> menus() {
        return R.success(sysMenuService.getTreedMenuByRole(List.of()));
    }



}

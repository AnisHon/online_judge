package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.dto.UserDto;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.user.domain.dto.SysUserDto;
import com.anishan.user.domain.dto.UserPagedQuery;
import com.anishan.api.domain.SysUser;
import com.anishan.user.domain.vo.UserVo;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;


@Api("用户实体操作接口，删改查")
@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    private final SysUserService sysUserService;

    @Autowired
    public UserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }


    @GetMapping("/username/{username}")
    @ApiOperation("查看用户名是否可用")
    public R<Boolean> availableUsername(@PathVariable("username") @NotNull String username) {
        boolean b = sysUserService.existsUsername(username);
        return R.success(!b);
    }

    @GetMapping("/email/{email}")
    @ApiOperation("查看邮箱是否可用")
    public R<Boolean> availableEmail(@PathVariable("email") @NotNull String email) {
        boolean b = sysUserService.existsEmail(email);
        return R.success(!b);
    }


    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("通过id获取用户")
    public R<UserVo> getUserById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        UserVo user = sysUserService.getUserById(id);
        return R.success(user);
    }

    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("通过多个id获取用户，id之间用','隔开")
    public R<List<UserVo>> listUser(@PathVariable("ids") List<String> ids) {
        List<UserVo> users = sysUserService.listUserById(ids);
        return R.success(users);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("分页获取User")
    public R<PagedResult<UserVo>> listUsers(@Validated @NotNull PagedQuery<SysUser> pagedQuery) {
        PagedResult<UserVo> userVoPagedResult = sysUserService.listUsers(pagedQuery);
        return userVoPagedResult.toR();
    }

    @PostMapping("/query")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("查询User")
    public R<PagedResult<UserVo>> queryUser(@Validated  @NotNull @RequestBody UserPagedQuery userPagedQuery) {
        PagedResult<UserVo> result = sysUserService.queryUser(userPagedQuery);
        return result.toR();
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:user:edit')")
    @ApiOperation("更新User，不能更改密码和Id和用户名")
    public R<Boolean> update(@RequestBody @Validated({ValidationGroup.Update.class}) UserDto userDto) {
        boolean b = sysUserService.updateUser(userDto);
        return R.success(b);
    }

    @GetMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('user:user:remove')")
    @ApiOperation("删除用户")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
        boolean b = sysUserService.removeById(id);
        return R.success(b);
    }

    @GetMapping("/removeBatch/{ids}")
    @PreAuthorize("hasAuthority('user:user:remove')")
    @ApiOperation("删除用户")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysUserService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('user:user:add')")
    @ApiOperation("添加用户")
    public R<String> addUser(@RequestBody SysUserDto sysUserDto) {
        try {
            sysUserService.addUser(sysUserDto);
        } catch (RuntimeException e) {
            return R.error(400, e.getMessage());
        }
        return R.success();
    }

    // change myself
    // todo


}

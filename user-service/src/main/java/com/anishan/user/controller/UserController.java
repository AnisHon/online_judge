package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.dto.UserDto;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.user.domain.dto.PagedUserRoleQuery;
import com.anishan.user.domain.dto.SysUserDto;
import com.anishan.user.domain.dto.SysUserInfoDto;
import com.anishan.user.domain.dto.UserPagedQuery;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/rank/{limit}")
    @ApiOperation("查看排名,最大200，高了没用")
    public R<List<UserVo>> rank(@NotNull @PathVariable("limit") @ApiParam("前limit位") Integer limit) {
        List<UserVo> list = sysUserService.rank(limit);
        return R.success(list);
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
    public R<UserVo> getUserById(@PathVariable("id") @NotNull(message = "id不能为Null") Long id) {
        UserVo user = sysUserService.getUserById(id);
        return R.success(user);
    }

    @PostMapping("/getByRole")
    @PreAuthorize("hasAnyAuthority('user:user:list', 'user:role:list')")
    @ApiOperation("通过role获取用户")
    public R<PagedResult<UserVo>> getUserByRole(@RequestBody @Validated PagedUserRoleQuery pagedQuery) {
        PagedResult<UserVo> userVoPagedResult = sysUserService.getUserByRoleId(pagedQuery);
        return userVoPagedResult.toR();
    }


    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("通过多个id获取用户，id之间用','隔开")
    public R<List<UserVo>> listUser(@PathVariable("ids") List<Long> ids) {
        List<UserVo> users = sysUserService.listUserById(ids);
        return R.success(users);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:user:list')")
    @ApiOperation("分页获取User")
    public R<PagedResult<UserVo>> listUsers(@Validated @RequestBody @NotNull PagedQuery<SysUser> pagedQuery) {
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
    @ApiOperation("删除用户，由于其破坏性较大所以已经禁止删除")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
//        boolean b = sysUserService.removeById(id);
        return R.success(false);
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
    public R<String> addUser(@RequestBody @Validated(ValidationGroup.Insert.class) SysUserDto sysUserDto) {
        try {
            sysUserService.addUser(sysUserDto);
        } catch (RuntimeException e) {
            return R.error(400, e.getMessage());
        }
        return R.success();
    }

    // change myself
    @PostMapping("/change-myself")
    @ApiOperation("更改个人信息")
    public R<Boolean> changeMyself(@RequestBody @Validated SysUserInfoDto sysUserDto, @RequestHeader("user-id") Long userId) {


        boolean b = sysUserService.changeInfo(sysUserDto, userId);

        return R.success(b);
    }


}

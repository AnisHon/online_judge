package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysRole;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.dto.UserDto;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.commons.enumeration.UserState;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.user.config.UserConfig;
import com.anishan.user.domain.dto.*;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.service.SysRoleService;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.service.SysUserService;
import com.anishan.user.util.SseUtils;
import com.anishan.user.util.UserUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.query.MPJLambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2024-10-02 23:14:28
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

    private final String baseString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                      "abcdefghijklmnopqrstuvwxyz" +
                                      "0123456789";


    private final SysUserMapper sysUserMapper;
    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final PasswordEncoder passwordEncoder;
    private final UserConfig config;
    private final SysMenuService sysMenuService;
    private final AuthUtil authUtil;
    private final SseUtils sseUtils;
    private final UserConfig userConfig;





    @Override
    public UserVo getUserById(Long id) {
        SysUser sysUser = this.getById(id);
        return BeanUtil.copyProperties(sysUser, UserVo.class);
    }


    // +=操作使用事务
    @Transactional
    @Override
    public boolean addPoint(Long userId, BigDecimal point) {

        boolean b = sysUserMapper.addPoints(userId, point) > 0;
        UserPoint userPoint = getPoint(userId);
        sseUtils.sendMessage(userId, SseEvent.UpdatePoint, userPoint);
        return b;
    }


    @Override
    public List<UserVo> listUserById(List<Long> ids) {
        List<SysUser> sysUsers = sysUserMapper.selectBatchIds(ids);
        return BeanUtil.copyToList(sysUsers, UserVo.class);
    }

    @Override
    public PagedResult<UserVo> listUsers(PagedQuery<SysUser> pagedQuery) {

        Page<SysUser> page = pagedQuery.page();
        page = this.page(page);
        return PagedResult.build(page, UserVo.class);
    }

    @Override
    public PagedResult<UserVo> queryUser(UserPagedQuery userPagedQuery) {
        Wrapper<SysUser> wrapper = userPagedQuery.wrapper();
        Page<SysUser> page = userPagedQuery.page();
        page = page(page, wrapper);
        return PagedResult.build(page, UserVo.class);
    }



    @Override
    public boolean updateUser(SysUser user) {
        LocalDateTime updateTime = MysqlMappingUtils.getUpdateTime(
                this,
                SysUser::getUserId,
                user.getUserId(),
                SysUser::getUpdateTime);
        user.setUpdateTime(updateTime);
        return this.updateById(user);
    }

    @Override
    public boolean updateUser(UserDto userDto) {
        SysUser sysUser = BeanUtil.copyProperties(userDto, SysUser.class);
        return updateUser(sysUser);
    }

    @Override
    @Transactional
    public void addUser(SysUserDto sysUserDto) {
        boolean exists = sysRoleService.existRole(sysUserDto.getRole());

        ThrowUtil.illegalArgument(!exists, "角色不存在");

        // 自动填充字段，添加User，会抛出异常
        SysUser sysUser = doSaveUser(sysUserDto);

        // 添加关系
        SysUserRoleRelation sysUserRoleRelation = new SysUserRoleRelation(sysUser.getUserId(), sysUserDto.getRole());
        sysUserRoleService.save(sysUserRoleRelation);
    }

    @Override
    public SysUser getUserByUsernameOrEmail(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, username)
                .or()
                .eq(SysUser::getEmail, username)
        );
    }

    @Override
    public boolean existsUsername(String username) {

        // 内存中ROOT用户无法注册
        if (username.equals("root")) {
            return true;
        }


        long count = sysUserMapper.selectJoinCount(new MPJLambdaQueryWrapper<SysUser>()
                .disableLogicDel()
                .eq(SysUser::getUserName, username));
        return count > 0;
    }

    @Override
    public boolean existsEmail(String email) {
        long count = sysUserMapper.selectJoinCount(new MPJLambdaQueryWrapper<SysUser>()
                .disableLogicDel()
                .eq(SysUser::getEmail, email));
        return count > 0;
    }

    @Override
    public boolean existsId(Long id) {
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, id)
        );
        return count > 0;
    }

    // 忽然意识到，这里自己写SQL要更方便，而且这样查效率低，内存占用大，还有BUG，回头再说，目前能跑就行
    @Override
    public PagedResult<UserVo> queryUserWithin(UserPagedQuery userPagedQuery, List<Long> studentIds) {
        Page<SysUser> page = userPagedQuery.page();
        LambdaQueryWrapper<SysUser> wrapper = userPagedQuery.lambdaQueryWrapper();
        wrapper.in(CollectionUtil.isEmpty(studentIds), SysUser::getUserId, studentIds);

        page = this.page(page, wrapper);
        return PagedResult.build(page, UserVo.class);
    }

    @Override
    public PagedResult<UserVo> listStudentsOfTeacher(PagedQuery<SysUser> userPagedQuery) {
        Long userId = UserUtil.getUserId();
        return listStudentsOfTeacher(userId, userPagedQuery);
    }

    @Override
    public PagedResult<UserVo> listStudentsOfTeacher(Long teacherId, PagedQuery<SysUser> userPagedQuery) {
        Long curr = userPagedQuery.getCurrentPage();
        Long total = userPagedQuery.getPageSize();

        List<SysUser> sysUsers = sysUserMapper.selectByTeacherIdLimit(
                teacherId,
                (curr - 1) * total,
                total
        );

        PagedResult<UserVo> result = new PagedResult<>();
        result.setData(BeanUtil.copyToList(sysUsers, UserVo.class));
        result.setCurrentPage(curr);
        result.setPageSize(total);

        return result;
    }

    /**
     * 通过roleId分页查询user
     */
    @Override
    public PagedResult<UserVo> getUserByRoleId(PagedUserRoleQuery userQuery) {
        Page<SysUserRoleRelation> page = userQuery.page();

        List<UserVo> userVos = sysUserMapper.selectUserByUserRoleQuery(page, userQuery);

        return PagedResult.fromPage(page, userVos, page.getTotal());
    }

    @Override
    public List<UserVo> rank(Integer limit) {
        Page<SysUser> page = Page.of(1, limit);
        List<SysUser> users = this.list(
                page,
                new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getNikeName, SysUser::getPoints)
                        .orderByDesc(SysUser::getPoints)
        );
        return BeanUtil.copyToList(users, UserVo.class);
    }

    public LoginUser getLoginUser(Long userId) {
        SysUser sysUser = this.getById(userId);

        return getLoginUser(sysUser, sysUserRoleService, sysMenuService);
    }

    @Override
    public LoginUser getRootAccount() {

        List<SysMenu> menus = sysMenuService.list();
        List<String> authority = menus.stream().map(SysMenu::getPerms).collect(Collectors.toList());



        String password = passwordEncoder.encode(userConfig.getRootPassword());

        // magic number id 0 -> root
        SysUser sysUser = new SysUser();
        sysUser.setUserId(0L);
        sysUser.setUserName("root");
        sysUser.setPassword(password);
        sysUser.setNikeName("ROOT");
        sysUser.setEmail("root@example.invalid");
        sysUser.setStatus(UserState.NORMAL);


        LoginUser loginUser = new LoginUser();
        loginUser.setUser(sysUser);
        loginUser.setAuths(authority);

        return loginUser;
    }

    @NotNull
    public static LoginUser getLoginUser(SysUser sysUser, SysUserRoleService sysUserRoleService, SysMenuService sysMenuService) {
        List<SysRole> roles;
        List<String> authorities;
        try {
            roles = sysUserRoleService.getRolesByUserId(sysUser.getUserId());
        } catch (Exception e) {
            roles = new ArrayList<>();
        }

        try {
            authorities = sysMenuService.getAuthorities_(roles);
        } catch (Exception e) {
            authorities = new ArrayList<>();
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(sysUser);
        loginUser.setRoles(roles);
        loginUser.setAuths(authorities);

        return loginUser;
    }


    private void refreshUser(Long userId) {
        LoginUser loginUser = getLoginUser(userId);
        authUtil.cacheLoginUser(loginUser);
    }

    @Override
    public boolean changeInfo(SysUserInfoDto sysUserDto, Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setNikeName(sysUserDto.getNikeName());

        boolean b = this.updateById(sysUser);

        if (b)  {
            refreshUser(userId);
        }
        return b;
    }


    private SysUser doSaveUser(SysUserDto sysUserDto) {
        sysUserDto = doFillEmptyProperties(sysUserDto);
        SysUser sysUser = BeanUtil.copyProperties(sysUserDto, SysUser.class, "role");
        boolean save = this.save(sysUser);

        ThrowUtil.businessError(!save, "保存失败");

        return sysUser;
    }

    private SysUserDto doFillEmptyProperties(SysUserDto sysUserDto) {
        // 如果nike name没有就生成随机的
        if (StrUtil.isBlank(sysUserDto.getNikeName())) {
            sysUserDto.setNikeName(RandomUtil.randomString(baseString, 10));
        }
        if (StrUtil.isBlank(sysUserDto.getEmail())) {
            sysUserDto.setEmail(sysUserDto.getUserName() + "@" + sysUserDto.getRole() + ".com");
        }
        if (sysUserDto.getPassword() == null || sysUserDto.getPassword().length() < 8) {
            sysUserDto.setPassword(config.getDefaultPassword());
        }

        sysUserDto.setPassword(passwordEncoder.encode(sysUserDto.getPassword()));
        return sysUserDto;
    }


    @Override
    public UserPoint getPoint(Long userId) {
        BigDecimal point = this.getObj(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getPoints)
                        .eq(SysUser::getUserId, userId),
                x -> (BigDecimal) x
        );

        UserPoint userPoint = new UserPoint();
        userPoint.setPoint(point);
        return userPoint;

    }

    @Override
    public Map<Long, String> getNikeNameToMap(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Map.of();
        }

        List<SysUser> sysUsers = this.list(
                new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getUserId, SysUser::getNikeName)
                        .in(SysUser::getUserId, ids));

        HashMap<Long, String> map = new HashMap<>();
        for (SysUser sysUser : sysUsers) {
            map.put(sysUser.getUserId(), sysUser.getNikeName());
        }
        return map;
    }


}





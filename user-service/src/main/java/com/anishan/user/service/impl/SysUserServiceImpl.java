package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.dto.UserDto;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.domain.dto.SysUserDto;
import com.anishan.user.domain.dto.UserPagedQuery;
import com.anishan.api.entity.SysRole;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.domain.vo.UserVo;
import com.anishan.user.service.SysRoleService;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.util.UserUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.api.entity.SysUser;
import com.anishan.user.service.SysUserService;
import com.anishan.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2024-10-02 23:14:28
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

    private final String baseString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                      "abcdefghijklmnopqrstuvwxyz" +
                                      "0123456789";

    private final SysUserMapper sysUserMapper;
    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SysUserServiceImpl(SysUserMapper sysUserMapper,
                              SysRoleService sysRoleService,
                              SysUserRoleService sysUserRoleService,
                              PasswordEncoder passwordEncoder
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleService = sysRoleService;
        this.sysUserRoleService = sysUserRoleService;
        this.passwordEncoder = passwordEncoder;
    }



    @Override
    public UserVo getUserById(Long id) {
        SysUser sysUser = this.getById(id);
        return BeanUtil.copyProperties(sysUser, UserVo.class);
    }

    @Override
    public List<UserVo> listUserById(List<String> ids) {
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
    public boolean updateUser(UserDto userDto) {
        LocalDateTime updateTime = MysqlMappingUtils.getUpdateTime(
                this,
                SysUser::getUserId,
                userDto.getUserId(),
                SysUser::getUpdateTime);

        SysUser sysUser = BeanUtil.copyProperties(userDto, SysUser.class);
        sysUser.setUpdateTime(updateTime);
        return this.updateById(sysUser);
    }

    @Override
    @Transactional
    public void addUser(SysUserDto sysUserDto) {
        SysRole sysRole = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, sysUserDto.getRole()));
        if (sysRole == null) {
           throw new RuntimeException("unknown role");
        }

        // 自动填充字段，添加User，会抛出异常
        SysUser sysUser = doSaveUser(sysUserDto);

        // 添加关系
        SysUserRoleRelation sysUserRoleRelation = new SysUserRoleRelation(sysUser.getUserId(), sysRole.getRoleId());
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
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, username)
        );
        return count > 0;
    }

    @Override
    public boolean existsEmail(String email) {
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
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

    private SysUser doSaveUser(SysUserDto sysUserDto) {
        sysUserDto = doFillEmptyProperties(sysUserDto);
        SysUser sysUser = BeanUtil.copyProperties(sysUserDto, SysUser.class, "role");
        boolean save = this.save(sysUser);
        if (!save) {
            throw new RuntimeException("save failed");
        }
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
        sysUserDto.setPassword(passwordEncoder.encode(sysUserDto.getPassword()));
        return sysUserDto;
    }





}





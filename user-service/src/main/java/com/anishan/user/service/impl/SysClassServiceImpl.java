package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.entity.dto.ClassDto;
import com.anishan.user.entity.dto.ClassPagedQuery;
import com.anishan.user.entity.vo.BinaryResultOv;
import com.anishan.user.entity.vo.ClassVo;
import com.anishan.user.service.AuthenticationService;
import com.anishan.user.service.StudentClassService;
import com.anishan.user.service.TeacherClassService;
import com.anishan.user.util.UserUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysClass;
import com.anishan.user.service.SysClassService;
import com.anishan.user.mapper.SysClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_class(班级信息表)】的数据库操作Service实现
* @createDate 2024-10-03 01:02:36
*/
@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass>
    implements SysClassService{


    private final SysClassMapper sysClassMapper;
    private final StudentClassService studentClassService;
    private final AuthenticationService authenticationService;
    private final TeacherClassService teacherClassService;

    @Autowired
    public SysClassServiceImpl(
            SysClassMapper sysClassMapper,
            StudentClassService studentClassService,
            AuthenticationService authenticationService,
            TeacherClassService teacherClassService
    ) {
        this.sysClassMapper = sysClassMapper;
        this.studentClassService = studentClassService;
        this.authenticationService = authenticationService;
        this.teacherClassService = teacherClassService;
    }

    @Override
    public ClassVo getClassById(Long id) {
        SysClass sysClass = this.getById(id);
        return BeanUtil.copyProperties(sysClass, ClassVo.class);
    }

    @Override
    public List<ClassVo> listClassById(List<Long> ids) {
        List<SysClass> sysClasses = this.listByIds(ids);
        return BeanUtil.copyToList(sysClasses, ClassVo.class);
    }

    @Override
    public PagedResult<ClassVo> listClasses(PagedQuery<SysClass> pagedQuery) {
        Page<SysClass> page = pagedQuery.page();
        page = this.page(page);
        return PagedResult.build(page, ClassVo.class);
    }

    @Override
    public PagedResult<ClassVo> queryClass(ClassPagedQuery classPagedQuery) {
        Page<SysClass> page = classPagedQuery.page();
        Wrapper<SysClass> wrapper = classPagedQuery.wrapper();
        page = this.page(page, wrapper);
        return PagedResult.build(page, ClassVo.class);
    }

    @Override
    public boolean updateClass(ClassDto classDto) {

        LocalDateTime updateTime = MysqlMappingUtils.getUpdateTime(
                this,
                SysClass::getClassId,
                classDto.getClassId(),
                SysClass::getUpdateTime);
        SysClass sysClass = BeanUtil.copyProperties(classDto, SysClass.class);
        sysClass.setUpdateTime(updateTime);
        return this.updateById(sysClass);
    }



    @Override
    public boolean addClass(ClassDto classDto) {
        SysClass sysClass = BeanUtil.copyProperties(classDto, SysClass.class, "classId");
        int insert = sysClassMapper.insert(sysClass);
        return insert > 0;
    }

    public boolean isClassExisted(Long classId) {
        long count = this.count(new LambdaQueryWrapper<SysClass>().eq(SysClass::getClassId, classId));
        return count > 0;
    }

    private BinaryResultOv checkClass(Long classId) {
        if (!isClassExisted(classId)) {
            return BinaryResultOv.fail("班级不存在");
        }
        return BinaryResultOv.success("");
    }

    // join class
    @Override
    public BinaryResultOv joinClass(Long userId, Long classId) {
        BinaryResultOv binaryResultOv = checkClass(classId);
        if (!binaryResultOv.isSuccess()) {
            return binaryResultOv;
        }

        boolean b = studentClassService.joinClass(userId, classId);

        return BinaryResultOv.ternary(b, "加入成功", "已经在班级了");
    }

    @Override
    public BinaryResultOv joinClass(Long classId) {
        Long userId = authenticationService.myId();
        return joinClass(userId, classId);
    }

    @Override
    public BinaryResultOv quitClass(Long userId, Long classId) {
        BinaryResultOv binaryResultOv = checkClass(classId);
        if (!binaryResultOv.isSuccess()) {
            return binaryResultOv;
        }

        boolean b = studentClassService.quitClass(userId, classId);
        return BinaryResultOv.ternary(b, "退出成功", "失败，不在班级中");
    }

    @Override
    public BinaryResultOv quitClass(Long classId) {
        Long userId = authenticationService.myId();
        return quitClass(userId, classId);
    }

    @Override
    public PagedResult<ClassVo> listClassOfUser(ClassPagedQuery classPagedQuery) {
        Long userId = authenticationService.myId();
        return listClassOfUser(classPagedQuery, userId);
    }

    @Override
    public List<SysClass> listByIds(Collection<? extends Serializable> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return new ArrayList<>();
        }
        return super.listByIds(idList);
    }

    /**
     * userId -> classIds -> classes(Page, Query)
     */

    public PagedResult<ClassVo> doListClassVo(ClassPagedQuery classPagedQuery, List<Long> classIds) {
        if (CollectionUtil.isEmpty(classIds)) {
            return new PagedResult<>();
        }
        Page<SysClass> page = classPagedQuery.page();
        LambdaQueryWrapper<SysClass> wrapper = classPagedQuery.lambdaQueryWrapper();
        wrapper.in(!CollectionUtil.isEmpty(classIds), SysClass::getClassId, classIds);

        page = this.page(page, wrapper);

        return PagedResult.build(page, ClassVo.class);
    }

    @Override
    public PagedResult<ClassVo> listClassOfUser(ClassPagedQuery classPagedQuery, Long userId) {
        List<Long> classIds =  studentClassService.listClassIdsOfUser(userId);

        return doListClassVo(classPagedQuery, classIds);
    }

    @Override
    public PagedResult<ClassVo> listClassOfTeacher(ClassPagedQuery classPagedQuery) {
        Long id = authenticationService.myId();
        return listClassOfTeacher(classPagedQuery, id);
    }

    @Override
    public PagedResult<ClassVo> listClassOfTeacher(ClassPagedQuery classPagedQuery, Long userId) {
        List<Long> classIds = teacherClassService.listClassIdOfTeacher(userId);

        return doListClassVo(classPagedQuery, classIds);
    }

    @Transactional
    @Override
    public boolean createClass(ClassDto classDto) {
        SysClass sysClass = BeanUtil.copyProperties(classDto, SysClass.class, "classId");
        int b = sysClassMapper.insert(sysClass);
        Long userId = UserUtil.getUserId();
        teacherClassService.addTeacherForClass(userId, sysClass.getClassId());
        return b > 0;
    }


}





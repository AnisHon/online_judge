package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.entity.dto.ClassDto;
import com.anishan.user.entity.dto.ClassPagedQuery;
import com.anishan.user.entity.vo.ClassVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysClass;
import com.anishan.user.service.SysClassService;
import com.anishan.user.mapper.SysClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_class(班级信息表)】的数据库操作Service实现
* @createDate 2024-10-03 01:02:36
*/
@Service
public class SysClassServiceImpl extends ServiceImpl<SysClassMapper, SysClass>
    implements SysClassService{


    SysClassMapper sysClassMapper;

    @Autowired
    public SysClassServiceImpl(SysClassMapper sysClassMapper) {
        this.sysClassMapper = sysClassMapper;
    }

    @Override
    public ClassVo getClassById(Long id) {
        SysClass sysClass = this.getById(id);
        return BeanUtil.copyProperties(sysClass, ClassVo.class);
    }

    @Override
    public List<ClassVo> listClassById(List<String> ids) {
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
        if (classDto.getClassId() == null) {
            return false;
        }

        SysClass sysClass = BeanUtil.copyProperties(classDto, SysClass.class);
        return this.updateById(sysClass);
    }



    @Override
    public boolean addClass(ClassDto classDto) {
        SysClass sysClass = BeanUtil.copyProperties(classDto, SysClass.class);
        int insert = sysClassMapper.insert(sysClass);
        return insert > 0;
    }
}





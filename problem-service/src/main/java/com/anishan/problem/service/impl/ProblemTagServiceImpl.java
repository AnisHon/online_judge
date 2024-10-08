package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.entity.po.ProblemTagRelation;
import com.anishan.problem.service.ProblemTagService;
import com.anishan.problem.mapper.ProblemTagMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【problem_tag(标签 题目关系表)】的数据库操作Service实现
* @createDate 2024-10-08 11:37:20
*/
@Service
public class ProblemTagServiceImpl extends ServiceImpl<ProblemTagMapper, ProblemTagRelation>
    implements ProblemTagService{

}





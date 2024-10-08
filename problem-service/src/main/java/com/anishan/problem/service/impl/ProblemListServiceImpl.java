package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.entity.po.ProblemListRelation;
import com.anishan.problem.service.ProblemListService;
import com.anishan.problem.mapper.ProblemListMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【problem_list(题单 题目关系表)】的数据库操作Service实现
* @createDate 2024-10-08 11:37:12
*/
@Service
public class ProblemListServiceImpl extends ServiceImpl<ProblemListMapper, ProblemListRelation>
    implements ProblemListService{

}





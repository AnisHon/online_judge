package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.service.ProblemProblemListService;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【problem_problem_list(题单 题目关系表)】的数据库操作Service实现
* @createDate 2024-10-16 22:40:59
*/
@Service
public class ProblemProblemListServiceImpl extends ServiceImpl<ProblemProblemListMapper, ProblemProblemListRelation>
    implements ProblemProblemListService{

}





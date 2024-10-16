package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.service.ProblemService;
import com.anishan.problem.mapper.ProblemMapper;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【problem(题目主表，OJ题目有分表，非OJ不需要继续分表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem>
    implements ProblemService{

}





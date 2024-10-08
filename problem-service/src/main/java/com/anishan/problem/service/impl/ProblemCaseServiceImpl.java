package com.anishan.problem.service.impl;

import com.anishan.problem.entity.po.ProblemCase;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.service.ProblemCaseService;
import com.anishan.problem.mapper.ProblemCaseMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【problem_case(判题测试用例)】的数据库操作Service实现
* @createDate 2024-10-08 15:05:34
*/
@Service
public class ProblemCaseServiceImpl extends ServiceImpl<ProblemCaseMapper, ProblemCase>
    implements ProblemCaseService{

}





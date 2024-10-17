package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.ProblemList;
import com.anishan.problem.service.ProblemListService;
import com.anishan.problem.mapper.ProblemListMapper;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【problem_list(题单表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class ProblemListServiceImpl extends ServiceImpl<ProblemListMapper, ProblemList>
    implements ProblemListService{

    public boolean isExistId(Long id) {
        return this.exists(new LambdaQueryWrapper<ProblemList>()
                .eq(ProblemList::getListId, id)
        );
    }
}





package com.anishan.problem.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.service.UserContestService;
import com.anishan.problem.mapper.UserContestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【user_contest(比赛参加表)】的数据库操作Service实现
* @createDate 2024-10-21 10:52:08
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserContestServiceImpl extends ServiceImpl<UserContestMapper, UserContestRelation>
    implements UserContestService{


    private final UserContestMapper userContestMapper;


    @Override
    public boolean saveIgnore(List<UserContestRelation> relations) {
        if (CollUtil.isEmpty(relations)) {
            return false;
        }
        int count = userContestMapper.insertBatchIgnore(relations);
        return count > 0;
    }
}





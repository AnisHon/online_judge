package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.service.UserContestService;
import com.anishan.problem.mapper.UserContestMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【user_contest(比赛参加表)】的数据库操作Service实现
* @createDate 2024-10-21 10:52:08
*/
@Service
public class UserContestServiceImpl extends ServiceImpl<UserContestMapper, UserContestRelation>
    implements UserContestService{

}





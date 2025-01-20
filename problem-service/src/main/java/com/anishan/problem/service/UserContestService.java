package com.anishan.problem.service;

import com.anishan.problem.domain.entity.UserContestRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author anishan
* @description 针对表【user_contest(比赛参加表)】的数据库操作Service
* @createDate 2024-10-21 10:52:08
*/
public interface UserContestService extends IService<UserContestRelation> {

    boolean saveIgnore(List<UserContestRelation> relations);
}

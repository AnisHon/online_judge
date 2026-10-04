package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.UserSubmit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
* @author happy
* @description 针对表【user_submit(用户提交表)】的数据库操作Mapper
* @createDate 2025-01-18 22:44:03
* @Entity com.anishan.problem.domain.entity.UserSubmit
*/
public interface UserSubmitMapper extends BaseMapper<UserSubmit> {

    int insertIgnore(@Param("userId") Long userId,
                     @Param("contestId") Long contestId,
                     @Param("submitTime") LocalDateTime submitTime);

    int deleteByContestAndUser(@Param("contestId") Long contestId, @Param("userId") Long userId);

}




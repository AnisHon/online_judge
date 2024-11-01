package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 比赛参加表
 * @TableName user_contest
 */
@TableName(value ="user_contest")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContestRelation implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 比赛ID
     */
    private Long contestId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 
 * @TableName supplement_contest
 */
@TableName(value ="supplement_contest")
@Data
public class SupplementContest {
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    @NotNull
    private Long userId;

    /**
     * 比赛ID
     */
    @TableField(value = "contest_id")
    @NotNull
    private Long contestId;

    /**
     * 最迟提交时间，比赛不能补交
     */
    @TableField(value = "deadline")
    @NotNull
    private LocalDateTime deadline;

}
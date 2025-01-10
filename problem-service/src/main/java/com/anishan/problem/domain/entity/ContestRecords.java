package com.anishan.problem.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 比赛题目记录
 * </p>
 *
 * @author anishan
 * @since 2025-01-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("contest_records")
@ApiModel(value="ContestRecords对象", description="比赛题目记录")
public class ContestRecords implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long recordId;

    @ApiModelProperty(value = "比赛ID")
    private Long contestId;

    @ApiModelProperty(value = "用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "题目id")
    @TableField("problem_id")
    private Long problemId;

    @ApiModelProperty(value = "是否正确")
    @TableField("status")
    private boolean status;

    @ApiModelProperty(value = "最终得分")
    @TableField("score")
    private BigDecimal score;


}

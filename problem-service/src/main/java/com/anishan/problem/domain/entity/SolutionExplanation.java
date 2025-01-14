package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文件信息表
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "solution_explanation", autoResultMap = true)
@ApiModel(value="SolutionExplanation对象", description="文件信息表")
public class SolutionExplanation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "solution_id", type = IdType.AUTO)
    private Long solutionId;

    @ApiModelProperty(value = "题解标题")
    private String title;

    @ApiModelProperty(value = "对应题目")
    private Long problemId;

    @ApiModelProperty(value = "发送者ID")
    private Long userId;

    @ApiModelProperty(value = "是否置顶")
    private Boolean topUp;

    @ApiModelProperty(value = "是否尽自己可见")
    @TableField(value = "private")
    private Boolean private_;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}

package com.anishan.problem.domain.dto;

import com.anishan.commons.e.ValidationGroup;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 填空选择题答案表
 * @TableName choice_fill_answers
 */
@Data
@ApiModel("填空题选择题答案")
public class ChoiceFillAnswersDto implements Serializable {

    @NotNull(groups = ValidationGroup.Update.class)
    @ApiModelProperty("主键id")
    private Long answerId;

    /**
     * 选项或填空答案
     */
    @NotEmpty(groups = {ValidationGroup.Insert.class})
    private String answerText;

    /**
     * 填空题空格索引, 选择题ABCD索引 1表示A
     */
    @Min(value = 0, groups = {ValidationGroup.Insert.class})
    @Max(value = 1, groups = {ValidationGroup.Insert.class})
    private Integer isCorrect;

    /**
     * 对于填空题，标识是第几个空格（填空题专用）
     */
    @Min(value = 0, groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class})
    private Integer blankIndex;

    /**
     * 题目某个空的分数
     */
    @Min(value = 0, groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class})
    private BigDecimal score;

}
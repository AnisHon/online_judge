package com.anishan.problem.domain.vo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题单表
 * @TableName problem_list
 */

@Data
@ApiModel("题单")
public class ProblemListVo {

    @ApiModelProperty("主键")
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    private String listName;

    @ApiModelProperty("题单说明，字数不应该太多")
    private String description;

    private LocalDateTime createTime;

    @ApiModelProperty("题目")
    private List<ProblemListRelationVo> problems;

}
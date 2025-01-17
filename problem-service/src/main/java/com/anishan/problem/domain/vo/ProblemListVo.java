package com.anishan.problem.domain.vo;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 题单表
 * @TableName problem_list
 */

@Data
@ApiModel("题单")
public class ProblemListVo {

    @ApiModelProperty("主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    private String listName;

    @ApiModelProperty("题单说明，字数不应该太多")
    private String description;

    private LocalDateTime createTime;

}
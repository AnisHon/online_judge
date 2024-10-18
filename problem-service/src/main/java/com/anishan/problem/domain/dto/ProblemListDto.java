package com.anishan.problem.domain.dto;

import com.anishan.commons.e.ValidationGroup;
import com.anishan.problem.domain.vo.ProblemVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题单表
 * @TableName problem_list
 */

@Data
@ApiModel("题单")
public class ProblemListDto {

    @ApiModelProperty("主键")
    @NotNull(groups = ValidationGroup.Update.class)
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    @NotNull(groups = ValidationGroup.Insert.class)
    private String listName;

    @ApiModelProperty("题单说明，字数不应该太多")
    @NotNull(groups = ValidationGroup.Insert.class)
    private String description;

}
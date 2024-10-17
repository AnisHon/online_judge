package com.anishan.problem.domain.dto;

import com.anishan.commons.e.Difficulty;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.problem.domain.entity.Problem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("查询题目")
public class PagedProblem extends PagedQuery<Problem> {

    @ApiModelProperty("题目ID")
    private Long problemId;

    @ApiModelProperty("tag的id")
    private List<Long> tagIds;


}

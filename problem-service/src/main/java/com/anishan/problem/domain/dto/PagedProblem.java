package com.anishan.problem.domain.dto;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.enumeration.ProblemType;
import com.anishan.problem.domain.entity.Problem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("查询题目")
public class PagedProblem extends PagedQuery<Problem> {

    @ApiModelProperty("题目ID")
    private Long problemId;

    @ApiModelProperty("名字")
    private String title;

    @ApiModelProperty("类型")
    private ProblemType type;

    @ApiModelProperty("tag的id")
    private List<Long> tagIds;

}

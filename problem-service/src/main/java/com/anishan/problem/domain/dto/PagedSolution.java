package com.anishan.problem.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.problem.domain.entity.SolutionExplanation;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PagedSolution extends SortedPagedQuery<SolutionExplanation> {

    @ConditionColumn(value = "eq")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ConditionColumn(value = "eq")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;

}

package com.anishan.problem.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.problem.domain.entity.SolutionExplanation;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PagedSolution extends SortedPagedQuery<SolutionExplanation> {

    @ConditionColumn(value = "eq")
    private Long userId;

    @ConditionColumn(value = "eq")
    private Long problemId;

}

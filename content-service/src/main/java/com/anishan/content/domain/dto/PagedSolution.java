package com.anishan.content.domain.dto;

import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.content.domain.entity.SolutionExplanation;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PagedSolution extends SortedPagedQuery<SolutionExplanation> {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
}

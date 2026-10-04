package com.anishan.content.domain.dto;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.content.domain.entity.SolutionModerationAction;
public class ModerationPageQuery extends PagedQuery<SolutionModerationAction> {
    public ModerationPageQuery() { super(20L, 1L); }
}

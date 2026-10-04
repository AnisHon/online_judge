package com.anishan.content.domain.dto;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.content.domain.entity.SolutionComment;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("一级评论分页")
public class CommentPageQuery extends PagedQuery<SolutionComment> {
    public CommentPageQuery() {
        super(20L, 1L);
    }
}

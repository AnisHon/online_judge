package com.anishan.content.domain.dto;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("后台评论分页查询")
public class AdminCommentPageQuery extends PagedQuery<SolutionComment> {
    @Min(1)
    @ApiModelProperty("题解ID，可选")
    private Long solutionId;

    @ApiModelProperty("评论状态，可选")
    private CommentState state;

    public AdminCommentPageQuery() {
        super(20L, 1L);
    }
}

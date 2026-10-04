package com.anishan.content.domain.vo;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.content.domain.enumeration.CommentState;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class CommentVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long rootId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private UserSummaryVo author;
    private UserSummaryVo replyTo;
    private String content;
    private CommentState state;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long likeCount;
    private Boolean likedByMe;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyCount;
    private LocalDateTime createdAt;
    private Boolean canDelete;
}

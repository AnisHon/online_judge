package com.anishan.content.domain.vo;

import com.anishan.content.domain.enumeration.CommentState;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Restricted administration projection. Never returned by ordinary comment endpoints. */
@Data
@Accessors(chain = true)
public class AdminCommentVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long rootId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToUserId;
    private String content;
    private CommentState state;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deletedBy;
    private LocalDateTime deletedAt;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long likeCount;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyCount;
    private LocalDateTime createdAt;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long actionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;
    private String action;
    private String reason;
    private LocalDateTime actionAt;
}

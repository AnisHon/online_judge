package com.anishan.content.domain.entity;

import com.anishan.content.domain.enumeration.CommentState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("solution_comment")
public class SolutionComment implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "comment_id", type = IdType.ASSIGN_ID)
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
    private Long likeCount;
    private Long replyCount;
    private String clientRequestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

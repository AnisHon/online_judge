package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("comment_like")
public class CommentLike implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private Boolean active;
    private Boolean firstNotified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

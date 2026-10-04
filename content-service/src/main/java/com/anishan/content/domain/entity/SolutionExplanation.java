package com.anishan.content.domain.entity;

import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Content-owned solution metadata. Long version is the only optimistic version field. */
@Data
@Accessors(chain = true)
@TableName("solution_explanation")
public class SolutionExplanation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "solution_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    private String title;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private Boolean topUp;
    @TableField("private")
    private Boolean private_;
    @TableLogic(value = "0", delval = "1")
    private Boolean delFlag;
    private SolutionModerationState moderationState;
    private Boolean commentsOpen;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime firstPublishedAt;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

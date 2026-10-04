package com.anishan.content.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@ApiModel("题解摘要")
public class SolutionVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    private String title;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
    private String problemTitle;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String nikeName;
    private Boolean topUp;
    private Boolean private_;
    private Long likeCount;
    private boolean likedByMe;
    private Long commentCount;
    private Boolean commentsOpen;
    private SolutionModerationState moderationState;
    private String effectiveVisibility;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

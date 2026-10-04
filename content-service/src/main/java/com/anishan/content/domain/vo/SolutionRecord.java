package com.anishan.content.domain.vo;

import com.anishan.content.domain.enumeration.SolutionModerationState;
import lombok.Data;

import java.time.LocalDateTime;

/** Database row used inside the content domain, not serialized as an API response. */
@Data
public class SolutionRecord {
    private Long solutionId;
    private String title;
    private Long problemId;
    private Long userId;
    private Boolean topUp;
    private Boolean private_;
    private Boolean delFlag;
    private SolutionModerationState moderationState;
    private Long version;
    private Long likeCount;
    private Long commentCount;
    private Boolean commentsOpen;
    private LocalDateTime firstPublishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String content;
}

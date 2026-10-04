package com.anishan.user.domain.vo;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/** Safe inbox representation; never contains event payloads or internal errors. */
@Data
public class NotificationVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long notificationId;
    private String type;
    private UserSummaryVo actor;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;
    private String action;
    private String reason;
    private LocalDateTime occurredAt;
    private LocalDateTime readAt;
}

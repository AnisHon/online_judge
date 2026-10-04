package com.anishan.content.domain.vo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;
/** Owner responses populate only action/reason/createdAt; IDs/operators are admin-only. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModerationActionVo {
    @JsonSerialize(using = ToStringSerializer.class) private Long actionId;
    @JsonSerialize(using = ToStringSerializer.class) private Long solutionId;
    private String targetType;
    @JsonSerialize(using = ToStringSerializer.class) private Long targetId;
    @JsonSerialize(using = ToStringSerializer.class) private Long authorId;
    @JsonSerialize(using = ToStringSerializer.class) private Long operatorId;
    private String action;
    private String reason;
    private LocalDateTime createdAt;
}

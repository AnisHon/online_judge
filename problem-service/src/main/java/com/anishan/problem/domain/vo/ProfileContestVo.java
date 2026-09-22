package com.anishan.problem.domain.vo;

import com.anishan.commons.enumeration.ContestType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileContestVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long contestId;
    private String title;
    private ContestType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

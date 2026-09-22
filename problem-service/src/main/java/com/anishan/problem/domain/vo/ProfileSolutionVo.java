package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileSolutionVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    private String title;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
    private String problemTitle;
    private Boolean private_;
    private LocalDateTime createTime;
}

package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 个人主页已通过题目的轻量投影，只返回题目 ID 与难度。 */
@Data
public class ProfileProblemItemVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long problemId;
    private Integer difficulty;
}

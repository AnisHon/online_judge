package com.anishan.problem.domain.vo;

import lombok.Data;

@Data
public class ProblemStatistic {
    private Long problemId;
    private String title;
    private Integer rightNum;
    private Integer wrongNum;

}

package com.anishan.problem.domain.vo;

import lombok.Data;

import java.util.List;

/** 闭区间 [startDate, endDate]，含零提交日期，最多 366 天。 */
@Data
public class ProfileActivityHeatmapVo {
    private String startDate;
    private String endDate;
    private String timeZone;
    private List<ProfileActivityDayVo> days;
}

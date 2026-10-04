package com.anishan.problem.domain.vo;

import lombok.Data;

/** 单个北京时间自然日的提交总数，不包含提交明细。 */
@Data
public class ProfileActivityDayVo {
    private String date;
    private long count;
}

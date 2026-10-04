package com.anishan.problem.domain.vo;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("最终榜单用户条目")
public class RankEntryVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("比赛名次")
    private Long rank;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("稳定展示顺序")
    private Long rowPosition;

    @ApiModelProperty("安全用户摘要；用户已删除时为空")
    private UserSummaryVo user;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("最终分数，以十进制字符串返回")
    private String score;

    @ApiModelProperty("正确题数")
    private Integer correctCount;

    @ApiModelProperty("有效作答题数")
    private Integer answeredCount;

    @ApiModelProperty("是否交卷")
    private Boolean handedIn;
}

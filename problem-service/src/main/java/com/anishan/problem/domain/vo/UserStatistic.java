package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("分数响应")
public class UserStatistic {

    @ApiModelProperty("用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("昵称")
    private String nikeName;

    @ApiModelProperty("正确题目数")
    private Integer correctNum;

    @ApiModelProperty("错误题目数")
    private Integer wrongNum;

    @ApiModelProperty("没做题目数量")
    private Integer absentNum;

    @ApiModelProperty("分数")
    private BigDecimal score;

    @ApiModelProperty("是否提交")
    private boolean submitted;

    @ApiModelProperty("是否迟交")
    private boolean late;

    @ApiModelProperty("交卷时间")
    private LocalDateTime submitTime;

}

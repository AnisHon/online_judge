package com.anishan.problem.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("题目对应用户分数分布")
public class ProblemScore {

    @ApiModelProperty("用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nikeName;

    @ApiModelProperty("是否正确, null代表没交")
    private Boolean correct;

    @ApiModelProperty("得分,  null代表没交")
    private BigDecimal score;

}

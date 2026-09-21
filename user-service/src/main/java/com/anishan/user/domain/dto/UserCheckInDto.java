package com.anishan.user.domain.dto;


import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ApiModel("用户签到信息")
public class UserCheckInDto {

    @ApiModelProperty("用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nikeName;

    @ApiModelProperty("奖励积分个数")
    private BigDecimal rewardPoint;

    @ApiModelProperty("签到时间")
    private LocalDate signTime;


    @ApiModelProperty("连续签到天数")
    private Integer continuityDays;


}

package com.anishan.user.domain.vo;

import com.anishan.user.domain.entity.UserCheckIn;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@ApiModel("签到响应")
@NoArgsConstructor
@AllArgsConstructor
public class UserCheckInInfo {

    @ApiModelProperty("是否成功")
    private Boolean success;
    @ApiModelProperty("信息")
    private String msg;
    @ApiModelProperty("奖励")
    private BigDecimal award;
    @ApiModelProperty("签到记录")
    UserCheckIn userCheckIn;


    public static UserCheckInInfo alreadyChecked() {
        return new UserCheckInInfo(false, "已经签到了", null, null);
    }

    public static UserCheckInInfo successChecked(BigDecimal award, UserCheckIn userCheckIn) {
        return new UserCheckInInfo(true, "签到成功", award, userCheckIn);
    }
}

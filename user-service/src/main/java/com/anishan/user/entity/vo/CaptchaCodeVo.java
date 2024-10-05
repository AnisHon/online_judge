package com.anishan.user.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("图片验证码")
public class CaptchaCodeVo {

    @ApiModelProperty("验证码token")
    private String token;
    @ApiModelProperty("图片使用base64编码，可以直接使用")
    private String image;

}

package com.anishan.user.domain.vo;

import com.anishan.commons.entity.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Api("用于重置密码，邮箱的返回信息类")
@AllArgsConstructor
@NoArgsConstructor
public class AuthResultVo {

    @ApiModelProperty("是否成功")
    private boolean success;

    @ApiModelProperty("成功返回：重制成功，未成功返回原因")
    private String message;

    public static AuthResultVo success(String msg) {
        return new AuthResultVo(true, msg);
    }

    public static AuthResultVo fail(String msg) {
        return new AuthResultVo(false, msg);
    }

    public R<AuthResultVo> toR() {
        return R.success(this);
    }

}

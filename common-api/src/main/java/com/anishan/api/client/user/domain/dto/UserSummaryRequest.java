package com.anishan.api.client.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/** Internal batch lookup request; IDs are strings to preserve 64-bit precision. */
@Data
@ApiModel("安全用户摘要批量查询")
public class UserSummaryRequest {

    @NotNull(message = "用户ID列表不能为空")
    @Size(max = 100, message = "一次最多查询100个用户")
    @ApiModelProperty("十进制用户ID列表，最多100个")
    private List<@NotBlank(message = "用户ID不能为空") @Pattern(regexp = "[0-9]{1,19}", message = "用户ID格式不正确") String> userIds;
}

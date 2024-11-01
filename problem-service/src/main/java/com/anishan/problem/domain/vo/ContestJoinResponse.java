package com.anishan.problem.domain.vo;

import com.anishan.problem.domain.dto.ContestJoinRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel("参加比赛的响应结果")
@NoArgsConstructor
@AllArgsConstructor
public class ContestJoinResponse {

    @ApiModelProperty("是否参加成功")
    private boolean success;

    @ApiModelProperty("参加失败的信息")
    private String message;


    public static ContestJoinResponse success() {
        return new ContestJoinResponse(true, "success");
    }

    public static ContestJoinResponse fail(String message) {
        return new ContestJoinResponse(false, message);
    }

    public static ContestJoinResponse conditional(boolean condition, String message) {
        if (condition) {
            return success();
        } else {
            return fail(message);
        }
    }

}

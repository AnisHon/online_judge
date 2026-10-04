package com.anishan.problem.domain.vo;

import com.anishan.commons.domain.vo.PagedResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("比赛最终榜单")
public class FinalRankVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("比赛ID")
    private Long contestId;

    @ApiModelProperty("结算状态：WAITING、BUILDING、READY、ERROR")
    private String state;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("已发布榜单版本")
    private Long version;

    @ApiModelProperty("计分规则版本")
    private String ruleVersion;

    @ApiModelProperty("成绩来源：CURRENT 或 LEGACY")
    private String sourceMode;

    @ApiModelProperty("当前已发布版本生成时间")
    private LocalDateTime generatedAt;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("未完成判题数量")
    private Long pendingCount;

    @ApiModelProperty("无已发布版本时为null；重建期间继续返回旧版本")
    private PagedResult<RankEntryVo> ranking;

    @ApiModelProperty("面向用户的安全状态提示")
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty("仅具有榜单管理权限的响应包含该诊断信息")
    private AdminBuildInfo adminBuildInfo;

    @Data
    @ApiModel("最终榜单管理诊断信息")
    public static class AdminBuildInfo {
        private Integer buildAttempts;
        private LocalDateTime nextBuildAt;
        private LocalDateTime leaseUntil;
        private String lastError;
    }
}

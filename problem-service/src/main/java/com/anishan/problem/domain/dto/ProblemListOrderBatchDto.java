package com.anishan.problem.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题单题目顺序批量更新请求。
 */
@Data
@ApiModel("题单题目顺序批量更新请求")
public class ProblemListOrderBatchDto {

    @NotNull
    @ApiModelProperty("题单 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long listId;

    @NotNull
    @ApiModelProperty("提交排序时读取到的题单更新时间，用于乐观锁校验")
    private LocalDateTime expectedUpdateTime;

    @Valid
    @NotEmpty
    @ApiModelProperty("题单内完整的题目顺序")
    private List<ProblemListOrderItemDto> items;
}

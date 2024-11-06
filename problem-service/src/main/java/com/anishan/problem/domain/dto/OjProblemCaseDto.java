package com.anishan.problem.domain.dto;

import com.anishan.commons.e.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("OJ判题测试用例")
public class OjProblemCaseDto implements Serializable {

    @ApiModelProperty("主键id")
    @NotNull(groups = {ValidationGroup.Update.class})
    private Long caseId;

    @NotEmpty(groups = {ValidationGroup.Insert.class})
    @ApiModelProperty("测试样例的输入")
    private String input;


    @NotEmpty(groups = {ValidationGroup.Insert.class})
    @ApiModelProperty("测试样例的输出")
    private String output;

    @Min(value = 0, groups = {ValidationGroup.Insert.class})
    @ApiModelProperty("答对的分数")
    private BigDecimal score;

}
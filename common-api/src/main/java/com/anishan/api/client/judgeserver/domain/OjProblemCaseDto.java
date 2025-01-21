package com.anishan.api.client.judgeserver.domain;

import com.anishan.commons.enumeration.ValidationGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

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

    @ApiModelProperty("问题ID")
    private Long problemId;

    @ApiModelProperty("输入文件")
    private MultipartFile inputFile;

    @ApiModelProperty("输出文件")
    private MultipartFile outputFile;

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
package com.anishan.user.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("班级视图表")
public class ClassVo {


    @ApiModelProperty("班级Id")
    private Long classId;

    @ApiModelProperty("班级名")
    private String className;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("备注")
    private String remark;


}

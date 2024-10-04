package com.anishan.user.entity.vo;

import com.baomidou.mybatisplus.core.conditions.update.Update;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@ApiModel("班级视图表")
public class ClassVo {


    @ApiModelProperty("班级Id")
    private Long classId;

    @ApiModelProperty("班级名")
    private String className;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("备注")
    private String remark;


}

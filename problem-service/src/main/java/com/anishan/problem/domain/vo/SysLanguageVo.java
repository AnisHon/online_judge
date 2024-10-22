package com.anishan.problem.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value ="sys_language")
@Data
@ApiModel("编程语言")
public class SysLanguageVo implements Serializable {

    @ApiModelProperty("主键")
    private Long languageId;

    @ApiModelProperty("语言名字")
    private String languageName;

    @ApiModelProperty("编译指令")
    private String compileCommand;

    @ApiModelProperty("语言排序")
    private Integer seq;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;


}
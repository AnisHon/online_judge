package com.anishan.problem.domain.entity;

import cn.hutool.json.ObjectMapper;
import com.anishan.problem.domain.vo.UserAnswer;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 比赛完成记录表，分表专门用于存储答案
 * </p>
 *
 * @author anishan
 * @since 2025-01-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "contest_answer_records", autoResultMap = true)
@ApiModel(value="ContestAnswerRecords对象", description="比赛完成记录表，分表专门用于存储答案")
public class ContestAnswerRecords implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    @TableField(typeHandler = JacksonTypeHandler.class, value = "answer")
    private UserAnswer answer;

}

package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** The original TEXT body is copied byte-for-byte from the legacy solution table. */
@Data
@Accessors(chain = true)
@TableName("solution_explanation_content")
public class SolutionExplanationContent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "solution_id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long solutionId;
    private String content;
}

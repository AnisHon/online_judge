package com.anishan.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("faq")
public class Faq implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "faq_id", type = IdType.ASSIGN_ID)
    private Long faqId;

    private String question;

    private String answer;

    @TableLogic
    private Boolean delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

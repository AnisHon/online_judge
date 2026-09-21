package com.anishan.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName user_check_in
 */
@TableName(value ="user_check_in")
@Data
public class UserCheckIn implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 奖励积分个数
     */
    private BigDecimal rewardPoint;

    /**
     * 签到时间
     */
    private LocalDate signTime;

    @TableField("current_time_")
    private LocalDateTime currentTime;

    /**
     * 连续签到天数
     */
    private Integer continuityDays;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

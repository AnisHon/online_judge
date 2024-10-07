package com.anishan.user.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生班级关系表
 * @TableName student_class
 */
@TableName(value ="student_class")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentClassRelation implements Serializable {
    /**
     * 学生ID(user_id)
     */
    private Long studentId;

    /**
     * 班级ID
     */
    private Long classId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
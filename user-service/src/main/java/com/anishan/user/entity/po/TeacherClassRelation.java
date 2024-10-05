package com.anishan.user.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师班级关系表
 * @TableName teacher_class
 */
@TableName(value ="teacher_class")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherClassRelation implements Serializable {
    /**
     * 教师ID(user_id)
     */
    private Long teacherId;

    /**
     * 班级ID
     */
    private Long classId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 题单 题目关系表
 * @TableName problem_problem_list
 */
@TableName(value ="problem_problem_list")
@Data
public class ProblemProblemListRelation implements Serializable {
    /**
     * 单子id
     */
    @TableId
    private Long listId;

    /**
     * 题目id
     */
    @TableId
    private Long problemId;

    /**
     * 题目顺序
     */
    private Integer problemOrder;

    /**
     * 每道题对应分数
     */
    private BigDecimal score;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ProblemProblemListRelation other = (ProblemProblemListRelation) that;
        return (this.getListId() == null ? other.getListId() == null : this.getListId().equals(other.getListId()))
            && (this.getProblemId() == null ? other.getProblemId() == null : this.getProblemId().equals(other.getProblemId()))
            && (this.getProblemOrder() == null ? other.getProblemOrder() == null : this.getProblemOrder().equals(other.getProblemOrder()))
            && (this.getScore() == null ? other.getScore() == null : this.getScore().equals(other.getScore()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getListId() == null) ? 0 : getListId().hashCode());
        result = prime * result + ((getProblemId() == null) ? 0 : getProblemId().hashCode());
        result = prime * result + ((getProblemOrder() == null) ? 0 : getProblemOrder().hashCode());
        result = prime * result + ((getScore() == null) ? 0 : getScore().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", listId=").append(listId);
        sb.append(", problemId=").append(problemId);
        sb.append(", problemOrder=").append(problemOrder);
        sb.append(", score=").append(score);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
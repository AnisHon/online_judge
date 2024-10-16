package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * OJ判题测试用例
 * @TableName oj_problem_case
 */
@TableName(value ="oj_problem_case")
@Data
public class OjProblemCase implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long caseId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 测试样例的输入
     */
    private String input;

    /**
     * 测试样例的输出
     */
    private String output;

    /**
     * 答对的分数
     */
    private Integer score;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     *
     */
    @Version
    private LocalDateTime updateTime;

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
        OjProblemCase other = (OjProblemCase) that;
        return (this.getCaseId() == null ? other.getCaseId() == null : this.getCaseId().equals(other.getCaseId()))
            && (this.getProblemId() == null ? other.getProblemId() == null : this.getProblemId().equals(other.getProblemId()))
            && (this.getInput() == null ? other.getInput() == null : this.getInput().equals(other.getInput()))
            && (this.getOutput() == null ? other.getOutput() == null : this.getOutput().equals(other.getOutput()))
            && (this.getScore() == null ? other.getScore() == null : this.getScore().equals(other.getScore()))
            && (this.getDelFlag() == null ? other.getDelFlag() == null : this.getDelFlag().equals(other.getDelFlag()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getCaseId() == null) ? 0 : getCaseId().hashCode());
        result = prime * result + ((getProblemId() == null) ? 0 : getProblemId().hashCode());
        result = prime * result + ((getInput() == null) ? 0 : getInput().hashCode());
        result = prime * result + ((getOutput() == null) ? 0 : getOutput().hashCode());
        result = prime * result + ((getScore() == null) ? 0 : getScore().hashCode());
        result = prime * result + ((getDelFlag() == null) ? 0 : getDelFlag().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", caseId=").append(caseId);
        sb.append(", problemId=").append(problemId);
        sb.append(", input=").append(input);
        sb.append(", output=").append(output);
        sb.append(", score=").append(score);
        sb.append(", delFlag=").append(delFlag);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
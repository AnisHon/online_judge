package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 填空选择题答案表
 * @TableName choice_fill_answers
 */
@TableName(value ="choice_fill_answers")
@Data
public class ChoiceFillAnswers implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long answerId;

    /**
     * 题目id
     */
    private Long problemId;

    /**
     * 选项或填空答案
     */
    private String answerText;

    /**
     * 是否为正确答案（选择题专用）默认false
     */
    private Integer isCorrect;

    /**
     * 对于填空题，标识是第几个空格（填空题专用）
     */
    private Integer blankIndex;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

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
        ChoiceFillAnswers other = (ChoiceFillAnswers) that;
        return (this.getAnswerId() == null ? other.getAnswerId() == null : this.getAnswerId().equals(other.getAnswerId()))
            && (this.getProblemId() == null ? other.getProblemId() == null : this.getProblemId().equals(other.getProblemId()))
            && (this.getAnswerText() == null ? other.getAnswerText() == null : this.getAnswerText().equals(other.getAnswerText()))
            && (this.getIsCorrect() == null ? other.getIsCorrect() == null : this.getIsCorrect().equals(other.getIsCorrect()))
            && (this.getBlankIndex() == null ? other.getBlankIndex() == null : this.getBlankIndex().equals(other.getBlankIndex()))
            && (this.getDelFlag() == null ? other.getDelFlag() == null : this.getDelFlag().equals(other.getDelFlag()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getAnswerId() == null) ? 0 : getAnswerId().hashCode());
        result = prime * result + ((getProblemId() == null) ? 0 : getProblemId().hashCode());
        result = prime * result + ((getAnswerText() == null) ? 0 : getAnswerText().hashCode());
        result = prime * result + ((getIsCorrect() == null) ? 0 : getIsCorrect().hashCode());
        result = prime * result + ((getBlankIndex() == null) ? 0 : getBlankIndex().hashCode());
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
        sb.append(", answerId=").append(answerId);
        sb.append(", problemId=").append(problemId);
        sb.append(", answerText=").append(answerText);
        sb.append(", isCorrect=").append(isCorrect);
        sb.append(", blankIndex=").append(blankIndex);
        sb.append(", delFlag=").append(delFlag);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
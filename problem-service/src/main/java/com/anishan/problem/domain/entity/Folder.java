package com.anishan.problem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 文件夹表
 * @TableName folder
 */
@TableName(value ="folder")
@Data
public class Folder implements Serializable {
    /**
     * 文件夹ID，不存在ID为0的文件夹
     */
    @TableId(type = IdType.AUTO)
    private Long folderId;

    /**
     * 唯一文件夹名
     */
    private String folderName;

    /**
     * 类型(D directory 目录，F file 文件, M 菜单栏)
     */
    private String folderType;

    /**
     * 父文件夹名，默认0表示没有父文件夹
     */
    private Long parentId;

    /**
     * 题单，如果是D类型则应该为空
     */
    private Long listId;

    /**
     * 逻辑删除
     */
    private Integer delFlag;

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
        Folder other = (Folder) that;
        return (this.getFolderId() == null ? other.getFolderId() == null : this.getFolderId().equals(other.getFolderId()))
            && (this.getFolderName() == null ? other.getFolderName() == null : this.getFolderName().equals(other.getFolderName()))
            && (this.getFolderType() == null ? other.getFolderType() == null : this.getFolderType().equals(other.getFolderType()))
            && (this.getParentId() == null ? other.getParentId() == null : this.getParentId().equals(other.getParentId()))
            && (this.getListId() == null ? other.getListId() == null : this.getListId().equals(other.getListId()))
            && (this.getDelFlag() == null ? other.getDelFlag() == null : this.getDelFlag().equals(other.getDelFlag()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFolderId() == null) ? 0 : getFolderId().hashCode());
        result = prime * result + ((getFolderName() == null) ? 0 : getFolderName().hashCode());
        result = prime * result + ((getFolderType() == null) ? 0 : getFolderType().hashCode());
        result = prime * result + ((getParentId() == null) ? 0 : getParentId().hashCode());
        result = prime * result + ((getListId() == null) ? 0 : getListId().hashCode());
        result = prime * result + ((getDelFlag() == null) ? 0 : getDelFlag().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", folderId=").append(folderId);
        sb.append(", folderName=").append(folderName);
        sb.append(", folderType=").append(folderType);
        sb.append(", parentId=").append(parentId);
        sb.append(", listId=").append(listId);
        sb.append(", delFlag=").append(delFlag);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
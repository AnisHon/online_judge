package com.anishan.problem.domain.vo;

import com.anishan.commons.e.FolderType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApiModel("树状TreeVo")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreedFolder {
    @ApiModelProperty("folder本体")
    private FolderVo folder;
    @ApiModelProperty("folder的下一级")
    private List<TreedFolder> children;

    public TreedFolder(FolderVo folder) {
        this.folder = folder;
        this.children = new ArrayList<>();
    }

    public boolean isFile() {
        return folder.getFolderType() == FolderType.File;
    }

    public void addChild(TreedFolder treedFolder) {
        if (treedFolder == null) {
            return;
        }
        children.add(treedFolder);
    }

}

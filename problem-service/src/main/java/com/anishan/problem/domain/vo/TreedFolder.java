package com.anishan.problem.domain.vo;

import com.anishan.commons.e.FolderType;
import io.swagger.annotations.ApiModel;
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

    private FolderVo folder;
    private List<TreedFolder> children;

    public TreedFolder(FolderVo folder) {
        this.folder = folder;
        this.children = new ArrayList<>();
    }

    public boolean isFolder() {
        return !(folder.getFolderType() == FolderType.Dictionary);
    }

    public boolean isParent(Long id) {
        return Objects.equals(folder.getParentId(), id);
    }
    public boolean isParent(TreedFolder treedFolder) {
        return isParent(treedFolder.folder.getFolderId());
    }

    public void addChild(TreedFolder treedFolder) {
        if (treedFolder == null) {
            return;
        }
        children.add(treedFolder);
    }

}

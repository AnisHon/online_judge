package com.anishan.problem.domain.dto;

import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.problem.domain.entity.ProblemList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;

@ApiModel("查询获取问题列表")
public class PagedProblemList extends SortedPagedQuery<ProblemList> {
    private static final Map<String, String> KEY_MAPPING;

    static {
        KEY_MAPPING = MysqlMappingUtils.mapColumn(PagedProblemList.class);
    }
    @ApiModelProperty("主键")
    @NotNull(groups = ValidationGroup.Update.class)
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    @NotNull(groups = ValidationGroup.Insert.class)
    private String listName;

    @Override
    protected Class<ProblemList> extendedClass() {
        return ProblemList.class;
    }

    @Override
    protected Object extendedObject() {
        return this;
    }

    @Override
    protected Map<String, String> extendedKeyMapping() {
        return KEY_MAPPING;
    }
}

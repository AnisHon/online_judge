package com.anishan.problem.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.problem.domain.entity.ProblemList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@ApiModel("查询获取问题列表")
@Data
public class PagedProblemList extends SortedPagedQuery<ProblemList> {

    @ApiModelProperty("主键")
    @NotNull(groups = ValidationGroup.Update.class)
    @ConditionColumn(value = "eq")
    private Long listId;

    @ApiModelProperty("题单名字，必须唯一")
    @NotNull(groups = ValidationGroup.Insert.class)
    @ConditionColumn
    private String listName;

}

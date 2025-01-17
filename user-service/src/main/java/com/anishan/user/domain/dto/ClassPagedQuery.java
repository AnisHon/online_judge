package com.anishan.user.domain.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.anishan.commons.domain.dto.SortedPagedQuery;
import com.anishan.user.domain.entity.SysClass;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("class分页")
public class ClassPagedQuery extends SortedPagedQuery<SysClass> {


    @SortedColumn
    @ConditionColumn("eq")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long classId;
    @SortedColumn
    @ConditionColumn
    private String className;


}

package com.anishan.user.entity.dto;

import com.anishan.commons.annotation.ConditionColumn;
import com.anishan.commons.annotation.SortedColumn;
import com.anishan.commons.entity.dto.SortedPagedQuery;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.entity.po.SysClass;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("class分页")
public class ClassPagedQuery extends SortedPagedQuery<SysClass> {

    private static final Map<String, String> KEY_MAPPING;

    static {
        KEY_MAPPING = MysqlMappingUtils.mapColumn(ClassPagedQuery.class);
    }


    @SortedColumn
    @ConditionColumn("eq")
    private Long classId;
    @SortedColumn
    @ConditionColumn()
    private String className;

    @Override
    protected Class<SysClass> extendedClass() {
        return SysClass.class;
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

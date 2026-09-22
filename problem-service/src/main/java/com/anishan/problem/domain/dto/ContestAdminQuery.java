package com.anishan.problem.domain.dto;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.problem.domain.entity.Contest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台竞赛/作业列表查询条件。
 *
 * <p>关键词和状态必须参与数据库分页，否则前端只过滤当前页会导致 total
 * 与实际结果不一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("后台竞赛查询参数")
public class ContestAdminQuery extends PagedQuery<Contest> {

    @ApiModelProperty("标题或说明关键词")
    private String keyword;

    @ApiModelProperty("状态：pending、running、ended")
    private String status;
}

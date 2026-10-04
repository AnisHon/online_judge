package com.anishan.problem.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;


/**
 * 个人主页的学习活动摘要。
 *
 * <p>这是有意设计成摘要而不是分页明细：主页只需要有限数量的入口，完整记录仍由原有业务接口负责。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProfileActivityVo extends ProfileActivityBaseVo {
    private boolean owner;

    /** Copy nested values so callers cannot mutate a shared hot-cache instance. */
    public static ProfileActivityVo fromBase(ProfileActivityBaseVo base, boolean owner) {
        ProfileActivityVo result = new ProfileActivityVo();
        BeanUtils.copyProperties(base, result);
        result.setOwner(owner);
        if (base.getSolvedProblems() != null) {
            result.setSolvedProblems(new LinkedHashMap<>());
            base.getSolvedProblems().forEach((key, ids) ->
                    result.getSolvedProblems().put(key, new ArrayList<>(ids)));
        }
        if (base.getContests() != null) {
            result.setContests(base.getContests().stream().map(item -> {
                ProfileContestVo copy = new ProfileContestVo();
                BeanUtils.copyProperties(item, copy);
                return copy;
            }).collect(Collectors.toList()));
        }
        if (base.getHeatmap() != null) {
            ProfileActivityHeatmapVo heatmap = new ProfileActivityHeatmapVo();
            BeanUtils.copyProperties(base.getHeatmap(), heatmap);
            heatmap.setDays(base.getHeatmap().getDays().stream().map(item -> {
                ProfileActivityDayVo copy = new ProfileActivityDayVo();
                BeanUtils.copyProperties(item, copy);
                return copy;
            }).collect(Collectors.toList()));
            result.setHeatmap(heatmap);
        }
        return result;
    }
}

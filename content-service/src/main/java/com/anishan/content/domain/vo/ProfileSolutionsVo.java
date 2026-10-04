package com.anishan.content.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.List;

@Data
public class ProfileSolutionsVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private List<ProfileSolutionVo> solutions;
    private boolean solutionsTruncated;
}

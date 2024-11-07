package com.anishan.problem.domain.vo;

import com.anishan.api.client.user.domain.vo.UserVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoredUser {

    private UserVo userVo;
    private BigDecimal score;
}

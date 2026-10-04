package com.anishan.content.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

/** Result of setting a solution-like state. Hidden solutions never expose their count. */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class LikeResultVo {
    private boolean liked;
    private Long likeCount;
}

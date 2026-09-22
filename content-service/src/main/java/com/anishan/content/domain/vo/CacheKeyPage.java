package com.anishan.content.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

/** 有界 Redis SCAN 结果，nextCursor 由客户端原样带回。 */
@Data
@Accessors(chain = true)
public class CacheKeyPage {

    private List<String> keys = Collections.emptyList();
    private String nextCursor = "0";
    private boolean hasMore;
}

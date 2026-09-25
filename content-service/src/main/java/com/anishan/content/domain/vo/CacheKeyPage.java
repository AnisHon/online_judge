package com.anishan.content.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

/** 有界缓存键分页结果；nextCursor 是下一页的偏移量。 */
@Data
@Accessors(chain = true)
public class CacheKeyPage {

    private List<String> keys = Collections.emptyList();
    private String nextCursor = "0";
    private boolean hasMore;
}

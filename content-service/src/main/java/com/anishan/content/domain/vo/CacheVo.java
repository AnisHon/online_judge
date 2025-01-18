package com.anishan.content.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CacheVo {

    private String key;

    private Long expireTime;

    private String value;

}

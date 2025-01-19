package com.anishan.content.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheTypeVo {
    private Integer id;
    private String type;
    private String desc;
}

package com.anishan.content.domain.vo;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.domain.PartHash;
import com.anishan.content.domain.dto.ChunkUploadDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SpliceVo extends ChunkUploadDto {

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private Long fileId;
    private String uploadId;
    private String path;
    private Boolean finished;
    private List<PartHash> partHashes;

    public static SpliceVo fromDto(ChunkUploadDto dto) {
        return BeanUtil.copyProperties(dto, SpliceVo.class);
    }

}

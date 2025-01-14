package com.anishan.content.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueryCloudFile {

    @NotNull
    private Long parentId;

    private String fileName;

}

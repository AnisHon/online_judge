package com.anishan.content.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueryCloudFile {

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String fileName;

}

package com.anishan.content.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Accessors(chain = true)
public class FaqDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long faqId;

    @NotBlank
    @Size(max = 500)
    private String question;

    @NotBlank
    @Size(max = 20000)
    private String answer;
}

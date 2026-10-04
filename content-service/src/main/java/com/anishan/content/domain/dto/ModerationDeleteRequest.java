package com.anishan.content.domain.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
@Data
public class ModerationDeleteRequest {
    @NotBlank private String reason;
}

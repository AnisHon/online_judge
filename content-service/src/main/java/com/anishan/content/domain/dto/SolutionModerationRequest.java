package com.anishan.content.domain.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
@Data
public class SolutionModerationRequest {
    public enum Action { AUTHOR_ONLY, RESTORE }
    @NotNull private Action action;
    @NotBlank private String reason;
}

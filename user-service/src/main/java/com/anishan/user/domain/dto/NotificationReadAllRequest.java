package com.anishan.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** IDs cross the browser boundary as decimal strings to preserve BIGINT precision. */
@Data
public class NotificationReadAllRequest {

    @NotBlank
    @Pattern(regexp = "[1-9][0-9]{0,18}")
    private String throughId;
}

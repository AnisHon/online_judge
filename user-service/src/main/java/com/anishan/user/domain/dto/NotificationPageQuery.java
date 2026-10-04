package com.anishan.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/** Recipient-scoped inbox pagination. */
@Data
public class NotificationPageQuery {

    @NotNull
    @Min(1)
    private Long currentPage = 1L;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer pageSize = 20;

    private Boolean unreadOnly = Boolean.FALSE;
}

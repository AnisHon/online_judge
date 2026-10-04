package com.anishan.user.domain.dto;

import lombok.Data;

/** Internal mapper projection; specialRole may be null when the user has no public role badge. */
@Data
public class UserSummaryRow {
    private Long userId;
    private String userName;
    private String nikeName;
    private String specialRole;
}

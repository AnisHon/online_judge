package com.anishan.problem.domain.dto;

import lombok.Data;

@Data
public class TestRequest {

    private Long languageId;
    private String code;
    private String stdin;
    private String uuid;


}

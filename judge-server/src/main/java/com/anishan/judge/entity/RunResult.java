package com.anishan.judge.entity;

import com.anishan.api.client.gojudge.enumeration.Status;
import lombok.Data;

@Data
public class RunResult {
    private Status status;
    private Integer exitStatus;
    private Long time;
    private Long memory;
    private Long runTime;
    private StdIOFile files;

    @Data
    public static class StdIOFile {
        private String stderr;
        private String stdout;
    }

}

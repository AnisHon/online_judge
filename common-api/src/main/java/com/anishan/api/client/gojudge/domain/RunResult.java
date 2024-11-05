package com.anishan.api.client.gojudge.domain;

import com.anishan.api.client.gojudge.enumeration.Status;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * [
 *     {
 *         "status": "Accepted",
 *         "exitStatus": 0,
 *         "time": 1879957,
 *         "memory": 262144,
 *         "runTime": 2349804,
 *         "files": {
 *             "stderr": "",
 *             "stdout": "2\n"
 *         }
 *     }
 * ]
 */
@ApiModel("运行结果")
@Data
public class RunResult {
    private Status status;
    private Integer exitStatus;
    private Long time;
    private Long memory;
    private Long runTime;
    private StdIoFile files;

    @Data
    public static class StdIoFile {
        private String stderr;
        private String stdout;
    }

}



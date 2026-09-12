package com.anishan.content.controller;

import com.anishan.api.service.CaseFileService;
import com.anishan.commons.domain.R;
import com.anishan.content.service.FileInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Api("content内部接口")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class InternalController {

    private final FileInfoService fileInfoService;

    private final CaseFileService caseFileService;

    @ResponseBody
    @ApiOperation("处理引用计数")
    @PostMapping("/reference")
    public void reference(@NotNull Map<String, Long> map) {
        fileInfoService.updateReference(map);
    }

}

package com.anishan.content.controller;

import com.anishan.commons.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对外提供站点级配置。
 *
 * <p>这里使用 Map 返回配置对象，后续增加站点配置项时不需要改变接口结构。</p>
 */
@RestController
@RequestMapping("/site")
@Api("站点接口")
public class SiteConfigController {

    @Value("${site.name:延拓Code}")
    private String siteName;

    @Value("${site.icp-number:津ICP备2025028121号-1}")
    private String icpNumber;

    @ApiOperation("站点配置")
    @GetMapping("/config")
    public R<Map<String, String>> config() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("siteName", siteName);
        config.put("icpNumber", icpNumber);
        return R.success(config);
    }
}

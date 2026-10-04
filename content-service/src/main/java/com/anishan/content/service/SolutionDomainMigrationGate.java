package com.anishan.content.service;

import com.anishan.content.mapper.ContentSolutionMigrationStateMapper;
import com.anishan.commons.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SolutionDomainMigrationGate {
    public static final String MIGRATION_KEY = "solution-domain-v1";

    private final ContentSolutionMigrationStateMapper stateMapper;

    public boolean isCutover() {
        try {
            return "CUTOVER".equals(stateMapper.selectStatus(MIGRATION_KEY));
        } catch (DataAccessException exception) {
            log.error("Unable to read solution migration state; keeping solution worker disabled", exception);
            throw new ApiStatusException(503, "题解服务暂不可用");
        }
    }

    public void requireCutover() {
        try {
            if ("CUTOVER".equals(stateMapper.selectStatus(MIGRATION_KEY))) {
                return;
            }
        } catch (DataAccessException exception) {
            log.error("Unable to read solution migration state; rejecting solution write", exception);
            throw new ApiStatusException(503, "题解服务暂不可用");
        }
        throw new ApiStatusException(503, "题解服务尚未完成切换");
    }
}

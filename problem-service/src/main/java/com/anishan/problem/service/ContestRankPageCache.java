package com.anishan.problem.service;

import com.anishan.problem.domain.entity.ContestRankEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Caches only immutable rank-page identifiers and scores; authorization is always checked by the caller. */
@Component
@Slf4j
public class ContestRankPageCache {

    public static final String PREFIX = "problem:contest:final-rank:v1:";
    public static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ContestRankPageCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public String key(Long contestId, Long version, long page, int size) {
        return PREFIX + contestId + ":" + version + ":" + page + ":" + size;
    }

    public PageSnapshot get(String key, Long contestId, Long version, long page, int size, long totalRecords) {
        final String json;
        try {
            json = redis.opsForValue().get(key);
        } catch (RuntimeException unavailable) {
            log.warn("Final-rank page cache read unavailable errorType={}", unavailable.getClass().getSimpleName());
            return null;
        }
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            PageSnapshot snapshot = objectMapper.readValue(json, PageSnapshot.class);
            if (!valid(snapshot, contestId, version, page, size, totalRecords)) {
                evictSingleKey(key);
                return null;
            }
            return snapshot;
        } catch (IOException | RuntimeException malformed) {
            evictSingleKey(key);
            log.warn("Invalid final-rank page cache entry discarded errorType={}", malformed.getClass().getSimpleName());
            return null;
        }
    }

    public void put(String key, PageSnapshot snapshot) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(snapshot), TTL);
        } catch (JsonProcessingException | RuntimeException unavailable) {
            log.warn("Final-rank page cache write unavailable errorType={}", unavailable.getClass().getSimpleName());
        }
    }

    public PageSnapshot snapshot(Long contestId, Long version, long page, int size,
                                 long totalRecords, List<ContestRankEntry> rows) {
        PageSnapshot snapshot = new PageSnapshot();
        snapshot.setContestId(contestId);
        snapshot.setVersion(version);
        snapshot.setPage(page);
        snapshot.setSize(size);
        snapshot.setTotalRecords(totalRecords);
        List<CachedRankEntry> entries = new ArrayList<>();
        for (ContestRankEntry row : rows) {
            CachedRankEntry entry = new CachedRankEntry();
            entry.setRank(row.getRankNo());
            entry.setRowPosition(row.getRowPosition());
            entry.setUserId(row.getUserId());
            entry.setScore(row.getScore() == null ? null : row.getScore().toPlainString());
            entry.setCorrectCount(row.getCorrectCount());
            entry.setAnsweredCount(row.getAnsweredCount());
            entry.setHandedIn(row.getHandedIn());
            entries.add(entry);
        }
        snapshot.setEntries(entries);
        return snapshot;
    }

    private boolean valid(PageSnapshot snapshot, Long contestId, Long version, long page, int size,
                          long totalRecords) {
        if (page < 1 || size < 1 || (page - 1) > Long.MAX_VALUE / size) {
            return false;
        }
        long offset = (page - 1) * size;
        long remaining = totalRecords - Math.min(totalRecords, offset);
        int expectedRows = (int) Math.min(size, remaining);
        if (snapshot == null || !contestId.equals(snapshot.getContestId())
                || !version.equals(snapshot.getVersion()) || snapshot.getPage() == null
                || snapshot.getPage() != page || snapshot.getSize() == null || snapshot.getSize() != size
                || snapshot.getTotalRecords() == null || snapshot.getTotalRecords() != totalRecords
                || snapshot.getEntries() == null || snapshot.getEntries().size() != expectedRows) {
            return false;
        }
        for (CachedRankEntry entry : snapshot.getEntries()) {
            if (entry == null || positiveOrNull(entry.getRank()) == false
                    || positiveOrNull(entry.getRowPosition()) == false
                    || positiveOrNull(entry.getUserId()) == false || entry.getScore() == null
                    || entry.getCorrectCount() == null || entry.getCorrectCount() < 0
                    || entry.getAnsweredCount() == null || entry.getAnsweredCount() < 0
                    || entry.getHandedIn() == null) {
                return false;
            }
            try {
                if (new BigDecimal(entry.getScore()).signum() < 0) {
                    return false;
                }
            } catch (NumberFormatException invalidScore) {
                return false;
            }
        }
        return true;
    }

    private boolean positiveOrNull(Long value) {
        return value != null && value > 0;
    }

    private void evictSingleKey(String key) {
        try {
            redis.delete(key);
        } catch (RuntimeException unavailable) {
            log.warn("Could not evict one invalid final-rank cache entry errorType={}",
                    unavailable.getClass().getSimpleName());
        }
    }

    @Data
    @NoArgsConstructor
    public static class PageSnapshot {
        private Long contestId;
        private Long version;
        private Long page;
        private Integer size;
        private Long totalRecords;
        private List<CachedRankEntry> entries;
    }

    @Data
    @NoArgsConstructor
    public static class CachedRankEntry {
        private Long rank;
        private Long rowPosition;
        private Long userId;
        private String score;
        private Integer correctCount;
        private Integer answeredCount;
        private Boolean handedIn;
    }
}

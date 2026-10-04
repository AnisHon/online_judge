package com.anishan.problem.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMapperTest {
    @Test
    void mapsOnlyBoundedDailyAggregatesAndKeepsTimeRangeIndexable() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mapper/ProfileMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "mapper/ProfileMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement(ProfileMapper.class.getName() + ".selectActivityDays");
        var bound = statement.getBoundSql(Map.of("userId", 42L,
                "startTime", LocalDateTime.of(2025, 10, 4, 0, 0),
                "endTime", LocalDateTime.of(2026, 10, 4, 0, 0)));
        String sql = bound.getSql().replaceAll("\\s+", " ").trim();
        assertTrue(sql.contains("count(*) as count"));
        assertTrue(sql.contains("user_id = ? and submit_time >= ? and submit_time < ?"));
        assertTrue(sql.contains("group by date_format(submit_time, '%Y-%m-%d')"));
        assertTrue(sql.endsWith("limit 366"));
        assertEquals(java.util.List.of("userId", "startTime", "endTime"),
                bound.getParameterMappings().stream().map(mapping -> mapping.getProperty()).collect(Collectors.toList()));
        assertFalse(configuration.hasStatement(ProfileMapper.class.getName() + ".selectSolutions"));
    }
}

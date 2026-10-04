package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestAttempt;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.domain.entity.ContestRankEntry;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContestRankSchemaTest {

    @Test
    void allContestMapperXmlFilesParseAndDeclareExpectedResultMaps() throws Exception {
        Configuration configuration = new Configuration();
        for (String file : new String[]{
                "ContestMapper.xml", "ContestRecordsService.xml", "SubmitLogMapper.xml",
                "JudgeCaseLogMapper.xml", "ContestProblemSnapshotMapper.xml", "ContestAttemptMapper.xml",
                "ContestRankSnapshotMapper.xml", "ContestRankEntryMapper.xml", "RecordsMapper.xml"
        }) {
            String resource = "mapper/" + file;
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        assertTrue(configuration.hasResultMap("com.anishan.problem.mapper.ContestProblemSnapshotMapper.ContestProblemSnapshotResultMap"));
        assertTrue(configuration.hasResultMap("com.anishan.problem.mapper.ContestAttemptMapper.ContestAttemptResultMap"));
        assertTrue(configuration.hasResultMap("com.anishan.problem.mapper.ContestRankSnapshotMapper.ContestRankSnapshotResultMap"));
        assertTrue(configuration.hasResultMap("com.anishan.problem.mapper.ContestRankEntryMapper.ContestRankEntryResultMap"));
        assertTrue(hasMappedProperty(configuration, "com.anishan.problem.mapper.ContestMapper.BaseResultMap",
                "scoringVersion"));
        assertTrue(hasMappedProperty(configuration, "com.anishan.problem.mapper.ContestRecordsMapper.ContestRecordsResultMap",
                "appliedAttemptSeq"));
        assertTrue(hasMappedProperty(configuration, "com.anishan.problem.mapper.SubmitLogMapper.SubmitLogResultMap",
                "resultApplied"));
        assertTrue(hasMappedProperty(configuration, "com.anishan.problem.mapper.SubmitLogMapper.SubmitLogResultMap",
                "completedAt"));

        assertEquals("contest_problem_snapshot", ContestProblemSnapshot.class.getAnnotation(TableName.class).value());
        assertEquals("contest_attempt", ContestAttempt.class.getAnnotation(TableName.class).value());
        assertEquals("contest_rank_snapshot", ContestRankSnapshot.class.getAnnotation(TableName.class).value());
        assertEquals("contest_rank_entry", ContestRankEntry.class.getAnnotation(TableName.class).value());
    }

    @Test
    void legacyRankSourceIsContestScopedAggregatedAndUnboundedByLimit() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/RecordsMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        BoundSql boundSql = configuration.getMappedStatement(
                RecordsMapper.class.getName() + ".selectUserStatistic")
                .getBoundSql(Collections.singletonMap("contestId", 2090000000000000001L));
        String sql = boundSql.getSql().toLowerCase().replaceAll("\\s+", " ");
        assertTrue(sql.contains("from user_contest uc"));
        assertTrue(sql.contains("contest_records"));
        assertTrue(sql.contains("where uc.contest_id = ?"));
        assertFalse(sql.contains(" limit "));
    }

    @Test
    void forwardMigrationPreservesLegacyScoresAndSeedsExplicitSourceModes() throws Exception {
        String sql = Files.readString(resolve("resources/sql/migration/V20261004_6__contest_final_rank.sql"))
                .toLowerCase(java.util.Locale.ROOT);
        assertTrue(sql.contains("scoring_version = 1"));
        assertTrue(sql.contains("'legacy_record_v1'"));
        assertTrue(sql.contains("'weighted_latest_v1'"));
        assertTrue(sql.contains("start_time is null or end_time is null"));
        assertTrue(sql.contains("state, version, next_version, build_attempts, next_build_at"));
        assertTrue(sql.contains("unique key uk_judge_case_log_submit_case"));
        assertTrue(sql.contains("unique key uk_contest_attempt_sequence"));
        assertTrue(sql.contains("judge_case_log_duplicate_backup_t17"));
        assertTrue(sql.contains("row_number() over"));
        assertTrue(sql.contains("order by j.update_time desc, j.id desc"));
        assertTrue(sql.contains("terminal_contest_submissions_requiring_maintenance_review"));
        assertFalse(sql.contains("update contest_records set score"));
        assertFalse(sql.contains("update submit_log set score"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("set result_applied = true"));
    }

    @Test
    void freshSchemaHasAllRankTablesAndCorrectContestRecordIndexTargets() throws Exception {
        String sql = Files.readString(resolve("resources/sql/db_problem.sql"))
                .toLowerCase(java.util.Locale.ROOT);
        for (String table : new String[]{"contest_problem_snapshot", "contest_attempt",
                "contest_rank_snapshot", "contest_rank_entry"}) {
            assertTrue(sql.contains("create table " + table), table);
        }
        assertTrue(sql.contains("create index contest_records_contest_id_idx on contest_records(contest_id)"));
        assertTrue(sql.contains("create index contest_records_user_id_idx on contest_records(user_id)"));
        assertTrue(sql.contains("create index contest_records_problem_id_idx on contest_records(problem_id)"));
        assertFalse(sql.contains("create index contest_records_contest_id_idx on records"));
        int contestRecordsStart = sql.indexOf("create table contest_records(");
        int rankSnapshotStart = sql.indexOf("create table contest_problem_snapshot", contestRecordsStart);
        String contestRecords = sql.substring(contestRecordsStart, rankSnapshotStart);
        assertTrue(contestRecords.contains("applied_attempt_seq"));
        assertTrue(contestRecords.contains("applied_attempt_id"));
        String ordinaryRecords = sql.substring(sql.indexOf("create table records("), contestRecordsStart);
        assertFalse(ordinaryRecords.contains("applied_attempt_seq"));
    }

    private Path resolve(String path) {
        Path fromRepositoryRoot = Paths.get(path);
        if (Files.exists(fromRepositoryRoot)) {
            return fromRepositoryRoot;
        }
        return Paths.get("../").resolve(path).normalize();
    }

    private boolean hasMappedProperty(Configuration configuration, String resultMapId, String property) {
        return configuration.getResultMap(resultMapId).getPropertyResultMappings().stream()
                .anyMatch(mapping -> property.equals(mapping.getProperty()));
    }
}

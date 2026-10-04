package com.anishan.problem.service;

import com.anishan.problem.config.ContestRankProperties;
import com.anishan.problem.domain.ContestRankBuildLease;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankEntryMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.service.impl.ContestFinalRankServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MySQL 8 integration tests; destructive DDL is permitted only in a loopback oj_t21_it schema. */
class ContestFinalRankRaceTest {

    private static final long CONTEST_ID = 2101L;
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 4, 10, 0);

    @Test
    void competingWorkersPublishOneVersionWithStableTiesAndRosterZeros() throws Exception {
        withMysql((fixture) -> {
            fixture.seedContest(2, 7L, END.minusSeconds(1));
            fixture.seedParticipants(101L, 102L, 103L);
            fixture.seedAttempt(501L, 101L, 201L, 1L, "NON_OJ", "COMPLETED", "AC", "10.00", 1);
            fixture.seedAttempt(502L, 101L, 202L, 2L, "NON_OJ", "COMPLETED", "WA", "0.00", 0);
            fixture.seedAttempt(503L, 102L, 201L, 3L, "NON_OJ", "COMPLETED", "AC", "10.00", 1);
            fixture.seedProjection(101L, 201L, "10.00", 1, 1);
            fixture.seedProjection(101L, 202L, "0.00", 0, 2);
            fixture.seedProjection(102L, 201L, "10.00", 1, 3);
            // A draft-only row must not contribute to the new version's score or counts.
            fixture.seedProjection(103L, 201L, "99.00", 1, 0);
            fixture.jdbc.update("INSERT INTO user_submit(user_id,contest_id,submit_time) VALUES(101,?,?)",
                    CONTEST_ID, END);

            ExecutorService workers = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            try {
                Future<ContestRankBuildLease> first = workers.submit(() -> fixture.claimAfterBarrier("worker-a", ready, start));
                Future<ContestRankBuildLease> second = workers.submit(() -> fixture.claimAfterBarrier("worker-b", ready, start));
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                ContestRankBuildLease firstLease = first.get(15, TimeUnit.SECONDS);
                ContestRankBuildLease secondLease = second.get(15, TimeUnit.SECONDS);
                assertNotEquals(firstLease == null, secondLease == null, "exactly one worker must own the lease");
                ContestRankBuildLease winner = firstLease == null ? secondLease : firstLease;
                assertNotNull(winner);

                long count = fixture.inTransaction(() -> fixture.service.buildCandidate(winner));
                assertEquals(3L, count);
                assertTrue(fixture.inTransaction(() -> fixture.service.publishCandidate(winner, count)));

                List<RankRow> rows = fixture.readRows(1L);
                assertEquals(3, rows.size());
                assertEquals(new RankRow(101L, 1L, 1L, "10.00", 1, 2, true), rows.get(0));
                assertEquals(new RankRow(102L, 1L, 2L, "10.00", 1, 1, false), rows.get(1));
                assertEquals(new RankRow(103L, 3L, 3L, "0.00", 0, 0, false), rows.get(2));
                ContestRankSnapshot header = fixture.readHeader();
                assertEquals(ContestRankState.READY, header.getState());
                assertEquals(1L, header.getVersion());
                assertEquals(3L, header.getTotalUsers());
                assertEquals(7L, header.getSourceSeq());
            } finally {
                workers.shutdownNow();
            }
        });
    }

    @Test
    void pendingJudgeWaitsThenErrorsWithoutInventingAResultAndLateResultCanPublish() throws Exception {
        withMysql((fixture) -> {
            fixture.seedContest(2, 1L, END.minusMinutes(5));
            fixture.seedParticipants(201L);
            fixture.seedOjAttempt(601L, 201L, 301L, 1L, "running", false, "PENDING");

            assertNull(fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "early-worker")));
            ContestRankSnapshot waiting = fixture.readHeader();
            assertEquals(ContestRankState.WAITING, waiting.getState());
            assertEquals(1L, waiting.getPendingCount());
            assertEquals(0L, fixture.jdbc.queryForObject("SELECT version FROM contest_rank_snapshot WHERE contest_id=?",
                    Long.class, CONTEST_ID));

            fixture.clock.set(END.plusMinutes(16));
            assertNull(fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "timeout-worker")));
            ContestRankSnapshot timedOut = fixture.readHeader();
            assertEquals(ContestRankState.ERROR, timedOut.getState());
            assertEquals(1L, timedOut.getPendingCount());
            assertTrue(timedOut.getLastError().contains("PENDING_TIMEOUT"));

            fixture.jdbc.update("UPDATE submit_log SET status='AC', result_applied=1, score=10.00, total_count=1, pass_count=1 WHERE submit_id=601");
            fixture.jdbc.update("UPDATE contest_attempt SET state='COMPLETED', judge_status='AC', score=10.00, correct=1 WHERE submit_id=601");
            fixture.seedProjection(201L, 301L, "10.00", 1, 1);
            fixture.clock.set(timedOut.getNextBuildAt().plusSeconds(1));
            ContestRankBuildLease lateLease = fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "late-result-worker"));
            assertNotNull(lateLease);
            long count = fixture.inTransaction(() -> fixture.service.buildCandidate(lateLease));
            assertEquals(1L, count);
            assertTrue(fixture.inTransaction(() -> fixture.service.publishCandidate(lateLease, count)));
            assertEquals(ContestRankState.READY, fixture.readHeader().getState());
            assertEquals(1L, fixture.readHeader().getVersion());
        });
    }

    @Test
    void expiredOwnerCannotPublishOrReuseItsCandidateVersion() throws Exception {
        withMysql((fixture) -> {
            fixture.seedContest(2, 0L, END.minusSeconds(1));
            fixture.seedParticipants(301L);
            ContestRankBuildLease initial = fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "first-owner"));
            long firstCount = fixture.inTransaction(() -> fixture.service.buildCandidate(initial));
            assertTrue(fixture.inTransaction(() -> fixture.service.publishCandidate(initial, firstCount)));
            assertEquals(1L, fixture.readHeader().getVersion());

            assertTrue(fixture.inTransaction(() -> fixture.service.requestRebuild(CONTEST_ID)));
            ContestRankBuildLease oldOwner = fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "old-owner"));
            assertNotNull(oldOwner);
            fixture.clock.advanceSeconds(121);
            ContestRankBuildLease newOwner = fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "new-owner"));
            assertNotNull(newOwner);
            assertNotEquals(oldOwner.getCandidateVersion(), newOwner.getCandidateVersion());

            assertThrows(IllegalStateException.class,
                    () -> fixture.inTransaction(() -> fixture.service.buildCandidate(oldOwner)));
            assertFalse(fixture.inTransaction(() -> fixture.service.publishCandidate(oldOwner, 0L)));
            assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM contest_rank_entry WHERE contest_id=? AND version=?",
                    Integer.class, CONTEST_ID, oldOwner.getCandidateVersion()));

            long newCount = fixture.inTransaction(() -> fixture.service.buildCandidate(newOwner));
            assertTrue(fixture.inTransaction(() -> fixture.service.publishCandidate(newOwner, newCount)));
            assertEquals(newOwner.getCandidateVersion(), fixture.readHeader().getVersion());
        });
    }

    @Test
    void rebuildFailureKeepsOldPublishedVersionAndLegacyUsesExistingProjection() throws Exception {
        withMysql((fixture) -> {
            fixture.seedContest(1, 0L, END.minusSeconds(1));
            fixture.seedParticipants(401L);
            fixture.seedProjection(401L, 401L, "17.77", 0, 0);
            ContestRankBuildLease legacy = fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "legacy-owner"));
            assertEquals("LEGACY", legacy.getSourceMode());
            assertEquals("LEGACY_RECORD_V1", legacy.getRuleVersion());
            long legacyCount = fixture.inTransaction(() -> fixture.service.buildCandidate(legacy));
            assertTrue(fixture.inTransaction(() -> fixture.service.publishCandidate(legacy, legacyCount)));
            assertEquals("17.77", fixture.readRows(1L).get(0).score);
            assertEquals("LEGACY", fixture.readHeader().getSourceMode());

            assertTrue(fixture.inTransaction(() -> fixture.service.requestRebuild(CONTEST_ID)));
            ContestRankBuildLease failed = fixture.inTransaction(() -> fixture.service.claim(CONTEST_ID, "failed-rebuild"));
            fixture.jdbc.update("UPDATE contest_records SET score=-1 WHERE contest_id=? AND user_id=401 AND problem_id=401", CONTEST_ID);
            assertThrows(RuntimeException.class, () -> fixture.inTransaction(() -> fixture.service.buildCandidate(failed)));
            fixture.inTransaction(() -> {
                fixture.service.markBuildFailed(failed, "SIMULATED_AGGREGATION_FAILURE");
                return null;
            });
            ContestRankSnapshot afterFailure = fixture.readHeader();
            assertEquals(ContestRankState.ERROR, afterFailure.getState());
            assertEquals(1L, afterFailure.getVersion());
            assertEquals("17.77", fixture.readRows(1L).get(0).score);
        });
    }

    private void withMysql(Scenario scenario) throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (Boolean.getBoolean("oj.community.integration.required")) {
            com.anishan.api.integration.CommunityIntegrationSupport.validateEnvironment();
            dataSource.setUrl("jdbc:mysql://" + com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_HOST")
                    + ":" + com.anishan.api.integration.CommunityIntegrationSupport.port("OJ_COMMUNITY_MYSQL_PORT")
                    + "/oj_it_problem?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
            dataSource.setUsername(com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_USER"));
            dataSource.setPassword(com.anishan.api.integration.CommunityIntegrationSupport.required("OJ_COMMUNITY_MYSQL_PASSWORD"));
        } else {
            String url = System.getenv("OJ_T21_MYSQL_URL");
            Assumptions.assumeTrue(url != null && !url.trim().isEmpty(),
                    "Set OJ_T21_MYSQL_URL to disposable loopback schema oj_t21_it");
            Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("OJ_T21_ALLOW_DESTRUCTIVE_FIXTURE")),
                    "Set OJ_T21_ALLOW_DESTRUCTIVE_FIXTURE=true only for the disposable test fixture");
            assertTrue(url.matches("(?i)^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):\\d+/oj_t21_it(?:[?].*)?$"),
                    "Refusing destructive DDL outside loopback-only oj_t21_it schema");
            dataSource.setUrl(url);
            dataSource.setUsername(environmentOrDefault("OJ_T21_MYSQL_USER", "root"));
            dataSource.setPassword(environmentOrDefault("OJ_T21_MYSQL_PASSWORD", ""));
        }
        createSchema(dataSource);
        try {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration);
            GlobalConfig global = new GlobalConfig();
            global.setIdentifierGenerator(new DefaultIdentifierGenerator());
            factory.setGlobalConfig(global);
            Resource[] mappers = new PathMatchingResourcePatternResolver().getResources(
                    "classpath*:mapper/{ContestMapper,ContestAttemptMapper,ContestRankSnapshotMapper,ContestRankEntryMapper}.xml");
            factory.setMapperLocations(mappers);
            SqlSessionFactory sqlSessionFactory = factory.getObject();
            SqlSessionTemplate sessions = new SqlSessionTemplate(sqlSessionFactory);
            TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            MutableClock clock = new MutableClock(END.plusMinutes(1));
            ContestRankProperties properties = new ContestRankProperties();
            properties.setLeaseSeconds(120);
            properties.setBatchSize(20);
            properties.setCleanupBatchSize(500);
            ContestFinalRankServiceImpl service = new ContestFinalRankServiceImpl(
                    sessions.getMapper(ContestMapper.class), sessions.getMapper(ContestAttemptMapper.class),
                    sessions.getMapper(ContestRankSnapshotMapper.class), sessions.getMapper(ContestRankEntryMapper.class),
                    properties, clock);
            Fixture fixture = new Fixture(dataSource, sessions, transactions, service, clock);
            scenario.run(fixture);
        } finally {
            dropSchema(dataSource);
        }
    }

    private void createSchema(DataSource dataSource) throws Exception {
        dropSchema(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE contest(contest_id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,title VARCHAR(255),list_id BIGINT NOT NULL,description LONGTEXT NULL,auth INT NOT NULL,type INT NOT NULL,pwd VARCHAR(255),start_time DATETIME NULL,end_time DATETIME NOT NULL,scoring_version INT NOT NULL,next_attempt_seq BIGINT NOT NULL,problem_snapshot_at DATETIME(3) NULL,submitted BOOLEAN NOT NULL DEFAULT 0,del_flag BOOLEAN NOT NULL DEFAULT 0,create_time DATETIME NULL,update_time DATETIME NULL) ENGINE=InnoDB");
            statement.execute("CREATE TABLE user_contest(user_id BIGINT NOT NULL,contest_id BIGINT NOT NULL,PRIMARY KEY(user_id,contest_id),KEY idx_user_contest_contest(contest_id,user_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE user_submit(user_id BIGINT NOT NULL,contest_id BIGINT NOT NULL,submit_time DATETIME NULL,PRIMARY KEY(user_id,contest_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_records(record_id BIGINT PRIMARY KEY,contest_id BIGINT NOT NULL,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,status BOOLEAN,score DECIMAL(10,3),applied_attempt_seq BIGINT NOT NULL DEFAULT 0,applied_attempt_id BIGINT NULL,UNIQUE KEY uk_contest_user_problem(contest_id,user_id,problem_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE submit_log(submit_id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,contest_id BIGINT NULL,status VARCHAR(32),score DECIMAL(10,2),total_count INT NULL,pass_count INT NULL,result_applied BOOLEAN NOT NULL DEFAULT 0) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_attempt(attempt_id BIGINT PRIMARY KEY,contest_id BIGINT NOT NULL,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,attempt_seq BIGINT NOT NULL,submit_id BIGINT NULL,kind VARCHAR(16) NOT NULL,accepted_at DATETIME(3) NOT NULL,state VARCHAR(16) NOT NULL,judge_status VARCHAR(32),score DECIMAL(10,2),correct BOOLEAN,completed_at DATETIME(3),UNIQUE KEY uk_attempt_seq(contest_id,attempt_seq),UNIQUE KEY uk_attempt_submit(submit_id),KEY idx_attempt_state(contest_id,state,attempt_seq)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_rank_snapshot(contest_id BIGINT PRIMARY KEY,state VARCHAR(16) NOT NULL DEFAULT 'WAITING',version BIGINT NOT NULL DEFAULT 0,next_version BIGINT NOT NULL DEFAULT 0,build_attempts INT NOT NULL DEFAULT 0,next_build_at DATETIME(3) NOT NULL,source_seq BIGINT NOT NULL DEFAULT 0,source_mode VARCHAR(24) NOT NULL,rule_version VARCHAR(24) NOT NULL,total_users BIGINT NOT NULL DEFAULT 0,pending_count BIGINT NOT NULL DEFAULT 0,lease_owner VARCHAR(64),lease_until DATETIME(3),generated_at DATETIME(3),last_error VARCHAR(1000),updated_at DATETIME(3) NOT NULL,KEY idx_rank_due(state,next_build_at,lease_until)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_rank_entry(contest_id BIGINT NOT NULL,version BIGINT NOT NULL,user_id BIGINT NOT NULL,rank_no BIGINT NOT NULL,row_position BIGINT NOT NULL,score DECIMAL(18,2) NOT NULL,correct_count INT NOT NULL DEFAULT 0,answered_count INT NOT NULL DEFAULT 0,handed_in BOOLEAN NOT NULL DEFAULT 0,created_at DATETIME(3) NOT NULL,PRIMARY KEY(contest_id,version,user_id),UNIQUE KEY uk_rank_position(contest_id,version,row_position),KEY idx_rank_page(contest_id,version,rank_no,row_position),CONSTRAINT chk_rank_values CHECK(score>=0 AND rank_no>0 AND row_position>0 AND correct_count>=0 AND answered_count>=0)) ENGINE=InnoDB");
        }
    }

    private void dropSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS contest_rank_entry");
            statement.execute("DROP TABLE IF EXISTS contest_rank_snapshot");
            statement.execute("DROP TABLE IF EXISTS contest_attempt");
            statement.execute("DROP TABLE IF EXISTS submit_log");
            statement.execute("DROP TABLE IF EXISTS contest_records");
            statement.execute("DROP TABLE IF EXISTS user_submit");
            statement.execute("DROP TABLE IF EXISTS user_contest");
            statement.execute("DROP TABLE IF EXISTS contest");
        }
    }

    private static String environmentOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null ? fallback : value;
    }

    @FunctionalInterface
    private interface Scenario {
        void run(Fixture fixture) throws Exception;
    }

    private static final class Fixture {
        private final JdbcTemplate jdbc;
        private final TransactionTemplate transactions;
        private final ContestFinalRankServiceImpl service;
        private final MutableClock clock;

        private Fixture(DataSource dataSource, SqlSessionTemplate sessions, TransactionTemplate transactions,
                        ContestFinalRankServiceImpl service, MutableClock clock) {
            this.jdbc = new JdbcTemplate(dataSource);
            this.transactions = transactions;
            this.service = service;
            this.clock = clock;
        }

        private void seedContest(int scoringVersion, long nextSeq, LocalDateTime endTime) {
            jdbc.update("INSERT INTO contest(contest_id,user_id,title,list_id,auth,type,end_time,scoring_version,next_attempt_seq,del_flag) VALUES(?,1,'fixture',1,0,0,?,?,?,0)",
                    CONTEST_ID, endTime, scoringVersion, nextSeq);
            String source = scoringVersion == 1 ? "LEGACY" : "CURRENT";
            String rule = scoringVersion == 1 ? "LEGACY_RECORD_V1" : "WEIGHTED_LATEST_V1";
            jdbc.update("INSERT INTO contest_rank_snapshot(contest_id,state,version,next_version,build_attempts,next_build_at,source_seq,source_mode,rule_version,total_users,pending_count,updated_at) VALUES(?,'WAITING',0,0,0,?,0,?,?,0,0,?)",
                    CONTEST_ID, clock.localNow(), source, rule, clock.localNow());
        }

        private void seedParticipants(Long... users) {
            for (Long userId : users) {
                jdbc.update("INSERT INTO user_contest(user_id,contest_id) VALUES(?,?)", userId, CONTEST_ID);
            }
        }

        private void seedProjection(long userId, long problemId, String score, int correct, long appliedSeq) {
            jdbc.update("INSERT INTO contest_records(record_id,contest_id,user_id,problem_id,status,score,applied_attempt_seq) VALUES(?,?,?,?,?,?,?)",
                    userId * 100 + problemId, CONTEST_ID, userId, problemId, correct, score, appliedSeq);
        }

        private void seedAttempt(long attemptId, long userId, long problemId, long attemptSeq,
                                 String kind, String state, String judgeStatus, String score, int correct) {
            jdbc.update("INSERT INTO contest_attempt(attempt_id,contest_id,user_id,problem_id,attempt_seq,kind,accepted_at,state,judge_status,score,correct) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP(3),?,?,?,?)",
                    attemptId, CONTEST_ID, userId, problemId, attemptSeq, kind, state, judgeStatus,
                    score, correct == 1);
        }

        private void seedOjAttempt(long submitId, long userId, long problemId, long attemptSeq,
                                   String status, boolean applied, String attemptState) {
            jdbc.update("INSERT INTO submit_log(submit_id,user_id,problem_id,contest_id,status,score,result_applied) VALUES(?,?,?,?,?,?,?)",
                    submitId, userId, problemId, CONTEST_ID, status, applied ? 10 : null, applied);
            jdbc.update("INSERT INTO contest_attempt(attempt_id,contest_id,user_id,problem_id,attempt_seq,submit_id,kind,accepted_at,state,judge_status) VALUES(?,?,?,?,?,?, 'OJ',CURRENT_TIMESTAMP(3),?,?)",
                    submitId + 1000, CONTEST_ID, userId, problemId, attemptSeq, submitId, attemptState, status);
        }

        private ContestRankBuildLease claimAfterBarrier(String owner, CountDownLatch ready, CountDownLatch start) throws Exception {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("worker barrier timed out");
            }
            return inTransaction(() -> service.claim(CONTEST_ID, owner));
        }

        private <T> T inTransaction(Supplier<T> action) {
            return transactions.execute(status -> action.get());
        }

        private ContestRankSnapshot readHeader() {
            return jdbc.queryForObject("SELECT contest_id,state,version,next_version,build_attempts,next_build_at,source_seq,source_mode,rule_version,total_users,pending_count,lease_owner,lease_until,generated_at,last_error,updated_at FROM contest_rank_snapshot WHERE contest_id=?",
                    (rs, row) -> {
                        ContestRankSnapshot value = new ContestRankSnapshot();
                        value.setContestId(rs.getLong("contest_id"));
                        value.setState(ContestRankState.valueOf(rs.getString("state")));
                        value.setVersion(rs.getLong("version"));
                        value.setNextVersion(rs.getLong("next_version"));
                        value.setBuildAttempts(rs.getInt("build_attempts"));
                        value.setNextBuildAt(rs.getTimestamp("next_build_at").toLocalDateTime());
                        value.setSourceSeq(rs.getLong("source_seq"));
                        value.setSourceMode(rs.getString("source_mode"));
                        value.setRuleVersion(rs.getString("rule_version"));
                        value.setTotalUsers(rs.getLong("total_users"));
                        value.setPendingCount(rs.getLong("pending_count"));
                        value.setLeaseOwner(rs.getString("lease_owner"));
                        java.sql.Timestamp lease = rs.getTimestamp("lease_until");
                        value.setLeaseUntil(lease == null ? null : lease.toLocalDateTime());
                        java.sql.Timestamp generated = rs.getTimestamp("generated_at");
                        value.setGeneratedAt(generated == null ? null : generated.toLocalDateTime());
                        value.setLastError(rs.getString("last_error"));
                        value.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                        return value;
                    }, CONTEST_ID);
        }

        private List<RankRow> readRows(long version) {
            return jdbc.query("SELECT user_id,rank_no,row_position,score,correct_count,answered_count,handed_in FROM contest_rank_entry WHERE contest_id=? AND version=? ORDER BY row_position",
                    (rs, row) -> new RankRow(rs.getLong("user_id"), rs.getLong("rank_no"),
                            rs.getLong("row_position"), rs.getBigDecimal("score").toPlainString(),
                            rs.getInt("correct_count"), rs.getInt("answered_count"), rs.getBoolean("handed_in")),
                    CONTEST_ID, version);
        }
    }

    private static final class RankRow {
        private final long userId;
        private final long rank;
        private final long rowPosition;
        private final String score;
        private final int correctCount;
        private final int answeredCount;
        private final boolean handedIn;

        private RankRow(long userId, long rank, long rowPosition, String score,
                        int correctCount, int answeredCount, boolean handedIn) {
            this.userId = userId;
            this.rank = rank;
            this.rowPosition = rowPosition;
            this.score = score;
            this.correctCount = correctCount;
            this.answeredCount = answeredCount;
            this.handedIn = handedIn;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RankRow)) return false;
            RankRow that = (RankRow) other;
            return userId == that.userId && rank == that.rank && rowPosition == that.rowPosition
                    && correctCount == that.correctCount && answeredCount == that.answeredCount
                    && handedIn == that.handedIn && score.equals(that.score);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, rank, rowPosition, score, correctCount, answeredCount, handedIn);
        }

        @Override
        public String toString() {
            return "RankRow{" + userId + ", rank=" + rank + ", score=" + score + '}';
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone = ZoneId.of("Asia/Shanghai");

        private MutableClock(LocalDateTime localDateTime) {
            this.instant = new AtomicReference<>(localDateTime.atZone(zone).toInstant());
        }

        private void set(LocalDateTime localDateTime) {
            instant.set(localDateTime.atZone(zone).toInstant());
        }

        private void advanceSeconds(long seconds) {
            instant.updateAndGet(value -> value.plusSeconds(seconds));
        }

        private LocalDateTime localNow() {
            return LocalDateTime.ofInstant(instant(), zone);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}

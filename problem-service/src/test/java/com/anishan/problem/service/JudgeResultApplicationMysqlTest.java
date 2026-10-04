package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeCaseResult;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.problem.config.JudgeConfig;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.JudgeCaseLogMapper;
import com.anishan.problem.mapper.ProblemCompleteMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
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
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Integration/race tests use only an explicitly allow-listed disposable loopback schema. */
class JudgeResultApplicationMysqlTest {

    private static final long CONTEST_ID = 201L;
    private static final long USER_ID = 202L;
    private static final long PROBLEM_ID = 203L;
    private static final String LOCK_TOKEN = "test-lock-token";

    @Test
    void concurrentDuplicateCallbacksApplyCasesProjectionAndPointsOnlyOnce() throws Exception {
        withMySql((sessions, transactions, jdbc) -> {
            Fixture fixture = fixture(sessions);
            seedContestSubmission(jdbc, 301L, 6L);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                Future<?> first = pool.submit(() -> applyAfterBarrier(transactions, fixture.service,
                        accepted(301L, 6L), ready, start));
                Future<?> second = pool.submit(() -> applyAfterBarrier(transactions, fixture.service,
                        accepted(301L, 6L), ready, start));
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                first.get(15, TimeUnit.SECONDS);
                second.get(15, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }

            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM submit_log WHERE submit_id=301 AND result_applied=TRUE", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM judge_case_log WHERE submit_id=301", Integer.class));
            assertEquals("COMPLETED", jdbc.queryForObject("SELECT state FROM contest_attempt WHERE submit_id=301", String.class));
            assertEquals(6L, jdbc.queryForObject("SELECT applied_attempt_seq FROM contest_records WHERE contest_id=201 AND user_id=202 AND problem_id=203", Long.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM problem_complete WHERE user_id=202 AND problem_id=203", Integer.class));
            SubmitLog persisted = sessions.getMapper(SubmitLogMapper.class).selectById(301L);
            assertEquals(JudgeResult.ACCEPT, persisted.getStatus());
            assertEquals(new BigDecimal("10.00"), persisted.getScore());
            assertEquals(1, persisted.getTotalCount());
            assertEquals(1, persisted.getPassCount());
            assertEquals(12L, persisted.getTime());
            assertEquals(32L, persisted.getMemory());
            transactions.execute(status -> {
                fixture.service.applyFinalResult(accepted(301L, 6L));
                return null;
            });
            verify(fixture.outbox, times(1)).recordPointAward(any(PointAwardEvent.class));
            verify(fixture.lock, times(3)).release(USER_ID, LOCK_TOKEN);
        });
    }

    @Test
    void laterAttemptRemainsTheProjectionWhenItsEarlierAcCallbackArrivesLate() throws Exception {
        withMySql((sessions, transactions, jdbc) -> {
            Fixture fixture = fixture(sessions);
            seedContestSubmission(jdbc, 401L, 4L);
            seedAdditionalSubmission(jdbc, 402L, 5L);

            transactions.execute(status -> {
                fixture.service.applyFinalResult(wrongAnswer(402L, 5L));
                return null;
            });
            transactions.execute(status -> {
                fixture.service.applyFinalResult(accepted(401L, 4L));
                return null;
            });

            ContestRecords projection = sessions.getMapper(ContestRecordsMapper.class)
                    .selectForUpdate(CONTEST_ID, USER_ID, PROBLEM_ID);
            assertEquals(5L, projection.getAppliedAttemptSeq());
            assertFalse(projection.getStatus());
            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getScore()));
            assertEquals("COMPLETED", jdbc.queryForObject("SELECT state FROM contest_attempt WHERE submit_id=401", String.class));
            assertEquals("COMPLETED", jdbc.queryForObject("SELECT state FROM contest_attempt WHERE submit_id=402", String.class));
            verify(fixture.outbox, times(1)).recordPointAward(any(PointAwardEvent.class));
        });
    }

    @Test
    void failureAfterCaseAndProjectionWritesRollsBackAllRowsAndCanBeRetried() throws Exception {
        withMySql((sessions, transactions, jdbc) -> {
            Fixture fixture = fixture(sessions);
            seedContestSubmission(jdbc, 501L, 9L);
            doThrow(new IllegalStateException("simulated points/outbox failure"))
                    .when(fixture.outbox).recordPointAward(any(PointAwardEvent.class));

            assertThrows(IllegalStateException.class, () -> transactions.execute(status -> {
                fixture.service.applyFinalResult(accepted(501L, 9L));
                return null;
            }));

            assertEquals(0, jdbc.queryForObject("SELECT result_applied FROM submit_log WHERE submit_id=501", Integer.class));
            assertEquals("PENDING", jdbc.queryForObject("SELECT state FROM contest_attempt WHERE submit_id=501", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM judge_case_log WHERE submit_id=501", Integer.class));
            assertEquals(0L, jdbc.queryForObject("SELECT applied_attempt_seq FROM contest_records WHERE contest_id=201 AND user_id=202 AND problem_id=203", Long.class));
            verify(fixture.lock, org.mockito.Mockito.never()).release(anyLong(), anyString());

            reset(fixture.outbox);
            transactions.execute(status -> {
                fixture.service.applyFinalResult(accepted(501L, 9L));
                return null;
            });
            assertEquals(1, jdbc.queryForObject("SELECT result_applied FROM submit_log WHERE submit_id=501", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM judge_case_log WHERE submit_id=501", Integer.class));
            assertEquals(9L, jdbc.queryForObject("SELECT applied_attempt_seq FROM contest_records WHERE contest_id=201 AND user_id=202 AND problem_id=203", Long.class));
            verify(fixture.outbox, times(1)).recordPointAward(any(PointAwardEvent.class));
        });
    }

    @Test
    void databaseUniqueCaseIndexRejectsRepeatedSubmitAndCaseIndex() throws Exception {
        withMySql((sessions, transactions, jdbc) -> {
            seedContestSubmission(jdbc, 601L, 2L);
            jdbc.update("INSERT INTO judge_case_log(id,submit_id,problem_id,case_id,case_index,status) VALUES(1,601,203,900,0,'AC')");
            assertThrows(Exception.class, () -> jdbc.update(
                    "INSERT INTO judge_case_log(id,submit_id,problem_id,case_id,case_index,status) VALUES(2,601,203,901,0,'AC')"));
        });
    }

    private Fixture fixture(SqlSessionTemplate sessions) {
        ProblemEventOutboxService outbox = mock(ProblemEventOutboxService.class);
        ProblemCompletionAwardService completion = new ProblemCompletionAwardService(
                sessions.getMapper(ProblemCompleteMapper.class), outbox, new JudgeConfig(), clock());
        RedisJudgeSubmissionLock lock = mock(RedisJudgeSubmissionLock.class);
        JudgeResultApplicationService service = new JudgeResultApplicationService(
                sessions.getMapper(ContestMapper.class), sessions.getMapper(SubmitLogMapper.class),
                sessions.getMapper(ContestAttemptMapper.class), sessions.getMapper(ContestRecordsMapper.class),
                sessions.getMapper(JudgeCaseLogMapper.class), mock(RecordsService.class), completion, lock, clock());
        return new Fixture(service, outbox, lock);
    }

    private void applyAfterBarrier(TransactionTemplate transactions, JudgeResultApplicationService service,
                                   JudgeScore callback, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("race test barrier timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        transactions.execute(status -> {
            service.applyFinalResult(callback);
            return null;
        });
    }

    private void seedContestSubmission(JdbcTemplate jdbc, long submitId, long attemptSeq) {
        jdbc.update("INSERT INTO contest(contest_id,user_id,title,list_id,auth,type,submitted,del_flag,scoring_version,next_attempt_seq) VALUES(201,1,'test',1,0,0,1,0,2,9)");
        jdbc.update("INSERT INTO contest_records(record_id,contest_id,user_id,problem_id,status,score,applied_attempt_seq) VALUES(701,201,202,203,0,0.00,0)");
        jdbc.update("INSERT INTO submit_log(submit_id,user_id,problem_id,language,contest_id,code,status,result_applied) VALUES(?,202,203,'java',201,'class Main {}','running',FALSE)", submitId);
        jdbc.update("INSERT INTO contest_attempt(attempt_id,contest_id,user_id,problem_id,attempt_seq,submit_id,kind,accepted_at,state) VALUES(?,?,?,?,?,?,'OJ',CURRENT_TIMESTAMP(3),'PENDING')",
                submitId + 1000, CONTEST_ID, USER_ID, PROBLEM_ID, attemptSeq, submitId);
    }

    private void seedAdditionalSubmission(JdbcTemplate jdbc, long submitId, long attemptSeq) {
        jdbc.update("INSERT INTO submit_log(submit_id,user_id,problem_id,language,contest_id,code,status,result_applied) VALUES(?,202,203,'java',201,'class Main {}','running',FALSE)", submitId);
        jdbc.update("INSERT INTO contest_attempt(attempt_id,contest_id,user_id,problem_id,attempt_seq,submit_id,kind,accepted_at,state) VALUES(?,?,?,?,?,?,'OJ',CURRENT_TIMESTAMP(3),'PENDING')",
                submitId + 1000, CONTEST_ID, USER_ID, PROBLEM_ID, attemptSeq, submitId);
    }

    private JudgeScore accepted(long submitId, long attemptSeq) {
        return callback(submitId, JudgeResult.ACCEPT, new BigDecimal("10.00"), 1,
                new JudgeCaseResult().setCaseId(900L + submitId).setCaseIndex(0).setStatus(JudgeResult.ACCEPT)
                        .setScore(new BigDecimal("10.000")).setTime(12L).setMemory(32L));
    }

    private JudgeScore wrongAnswer(long submitId, long attemptSeq) {
        return callback(submitId, JudgeResult.WRONG_ANSWER, BigDecimal.ZERO, 0,
                new JudgeCaseResult().setCaseId(900L + submitId).setCaseIndex(0).setStatus(JudgeResult.WRONG_ANSWER)
                        .setScore(BigDecimal.ZERO).setTime(12L).setMemory(32L));
    }

    private JudgeScore callback(long submitId, JudgeResult result, BigDecimal score, int passed,
                                JudgeCaseResult caseResult) {
        return new JudgeScore().setSubmitId(submitId).setUserId(USER_ID).setProblemId(PROBLEM_ID)
                .setContestId(CONTEST_ID).setSubmissionLockToken(LOCK_TOKEN).setResult(result).setScore(score)
                .setRuntime(12L).setMemory(32L).setTotalCount(1).setPassCount(passed)
                .setCaseResults(Collections.singletonList(caseResult));
    }

    private Clock clock() {
        return Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC);
    }

    private void withMySql(MySqlScenario scenario) throws Exception {
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
            String url = System.getenv("OJ_T20_MYSQL_URL");
            Assumptions.assumeTrue(url != null && !url.trim().isEmpty(),
                    "Set OJ_T20_MYSQL_URL to disposable loopback schema oj_t20_it");
            Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("OJ_T20_ALLOW_DESTRUCTIVE_FIXTURE")),
                    "Set OJ_T20_ALLOW_DESTRUCTIVE_FIXTURE=true only for the disposable test fixture");
            assertTrue(url.matches("(?i)^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):\\d+/oj_t20_it(?:[?].*)?$"),
                    "Refusing destructive DDL outside loopback-only oj_t20_it schema");
            dataSource.setUrl(url);
            dataSource.setUsername(environmentOrDefault("OJ_T20_MYSQL_USER", "root"));
            dataSource.setPassword(environmentOrDefault("OJ_T20_MYSQL_PASSWORD", ""));
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
                    "classpath*:mapper/{ContestMapper,SubmitLogMapper,ContestAttemptMapper,ContestRecordsService,JudgeCaseLogMapper,ProblemCompleteMapper}.xml");
            factory.setMapperLocations(mappers);
            SqlSessionFactory sqlSessionFactory = factory.getObject();
            SqlSessionTemplate sessions = new SqlSessionTemplate(sqlSessionFactory);
            TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            scenario.run(sessions, transactions, new JdbcTemplate(dataSource));
        } finally {
            dropSchema(dataSource);
        }
    }

    private void createSchema(DataSource dataSource) throws Exception {
        dropSchema(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE contest(contest_id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,title VARCHAR(255),list_id BIGINT NOT NULL,description LONGTEXT NULL,auth INT NOT NULL,type INT NOT NULL,pwd VARCHAR(255),start_time DATETIME NULL,end_time DATETIME NULL,submitted BOOLEAN NOT NULL,del_flag BOOLEAN NOT NULL DEFAULT 0,create_time DATETIME NULL,update_time DATETIME NULL,scoring_version INT NOT NULL DEFAULT 2,next_attempt_seq BIGINT NOT NULL DEFAULT 0,problem_snapshot_at DATETIME(3) NULL) ENGINE=InnoDB");
            statement.execute("CREATE TABLE submit_log(submit_id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,language VARCHAR(20) NOT NULL,contest_id BIGINT NULL,code LONGTEXT,status VARCHAR(32),time BIGINT,memory BIGINT,stderr TEXT,internal_error LONGTEXT,error_code VARCHAR(64),total_count INT,pass_count INT,score DECIMAL(10,2),result_applied BOOLEAN NOT NULL DEFAULT 0,completed_at DATETIME(3),submit_time DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_attempt(attempt_id BIGINT PRIMARY KEY,contest_id BIGINT NOT NULL,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,attempt_seq BIGINT NOT NULL,submit_id BIGINT NULL,kind VARCHAR(16) NOT NULL,accepted_at DATETIME(3) NOT NULL,state VARCHAR(16) NOT NULL DEFAULT 'PENDING',judge_status VARCHAR(32),score DECIMAL(10,2),correct BOOLEAN,completed_at DATETIME(3),UNIQUE KEY uk_contest_attempt_submit(submit_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_records(record_id BIGINT PRIMARY KEY,contest_id BIGINT NOT NULL,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,status BOOLEAN NOT NULL,score DECIMAL(10,3),applied_attempt_seq BIGINT NOT NULL DEFAULT 0,applied_attempt_id BIGINT NULL,UNIQUE KEY uk_contest_user_problem(contest_id,user_id,problem_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE judge_case_log(id BIGINT PRIMARY KEY,submit_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,case_id BIGINT NULL,case_index INT NOT NULL,status VARCHAR(32) NOT NULL,score DECIMAL(10,3) DEFAULT 0,time BIGINT DEFAULT 0,memory BIGINT DEFAULT 0,internal_error LONGTEXT,create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,UNIQUE KEY uk_judge_case_log_submit_case(submit_id,case_index)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE problem_complete(user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,PRIMARY KEY(user_id,problem_id)) ENGINE=InnoDB");
        }
    }

    private void dropSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS judge_case_log");
            statement.execute("DROP TABLE IF EXISTS contest_records");
            statement.execute("DROP TABLE IF EXISTS contest_attempt");
            statement.execute("DROP TABLE IF EXISTS submit_log");
            statement.execute("DROP TABLE IF EXISTS problem_complete");
            statement.execute("DROP TABLE IF EXISTS contest");
        }
    }

    private String environmentOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null ? fallback : value;
    }

    private static final class Fixture {
        private final JudgeResultApplicationService service;
        private final ProblemEventOutboxService outbox;
        private final RedisJudgeSubmissionLock lock;

        private Fixture(JudgeResultApplicationService service, ProblemEventOutboxService outbox,
                        RedisJudgeSubmissionLock lock) {
            this.service = service;
            this.outbox = outbox;
            this.lock = lock;
        }
    }

    @FunctionalInterface
    private interface MySqlScenario {
        void run(SqlSessionTemplate sessions, TransactionTemplate transactions, JdbcTemplate jdbc) throws Exception;
    }
}

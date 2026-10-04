package com.anishan.problem.service;

import com.anishan.problem.config.JudgeConfig;
import com.anishan.problem.domain.entity.ContestAnswerRecords;
import com.anishan.problem.domain.entity.ContestRecords;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.ProblemCompleteMapper;
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
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Destructive mapper/race fixtures are restricted to a loopback-only disposable MySQL schema. */
class ContestAnswerPersistenceMysqlTest {

    @Test
    void latestProjectionAndDraftAnswerWritesAreAtomicAndUseJsonMapper() throws Exception {
        withMySql((sessions, transactions, jdbc) -> {
            ContestRecordsMapper records = sessions.getMapper(ContestRecordsMapper.class);
            ContestAnswerRecordsMapper answers = sessions.getMapper(ContestAnswerRecordsMapper.class);

            assertEquals(1, records.insertDraftIfAbsent(1001L, 31L, 41L, 51L));
            assertEquals(0, records.insertDraftIfAbsent(1002L, 31L, 41L, 51L));
            records.upsertGradedProjection(1003L, 31L, 41L, 51L,
                    false, new BigDecimal("3.33"), 2L, 2002L);
            records.upsertGradedProjection(1004L, 31L, 41L, 51L,
                    true, BigDecimal.ZERO.setScale(2), 3L, 2003L);
            records.upsertGradedProjection(1005L, 31L, 41L, 51L,
                    false, new BigDecimal("9.99"), 1L, 2001L);

            ContestRecords projection = records.selectForUpdate(31L, 41L, 51L);
            assertEquals(1001L, projection.getRecordId());
            assertEquals(3L, projection.getAppliedAttemptSeq());
            assertEquals(2003L, projection.getAppliedAttemptId());
            assertTrue(projection.getStatus());
            assertEquals(0, new BigDecimal("0.00").compareTo(projection.getScore()));

            UserAnswer answer = new UserAnswer(Arrays.asList(
                    new com.anishan.problem.domain.dto.JudgeAnswer(1, "draft")), "class Main {}", 12L);
            answers.upsertAnswer(projection.getRecordId(), answer);
            ContestAnswerRecords saved = answers.selectById(projection.getRecordId());
            assertNotNull(saved);
            assertEquals("class Main {}", saved.getAnswer().getCode());
            assertEquals("draft", saved.getAnswer().getAnswers().get(0).getAnswer());

            // The draft insert path only initializes absent rows; existing grades remain untouched.
            records.insertDraftIfAbsent(1006L, 31L, 41L, 51L);
            ContestRecords afterDraft = records.selectForUpdate(31L, 41L, 51L);
            assertEquals(3L, afterDraft.getAppliedAttemptSeq());
            assertTrue(afterDraft.getStatus());
            assertEquals(0, new BigDecimal("0.00").compareTo(records.selectContestScore(31L, 41L)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM contest_attempt", Integer.class));
        });
    }

    @Test
    void concurrentFirstAcAndOutboxFailureRespectTheDatabaseTransaction() throws Exception {
        withMySql((sessions, transactions, jdbc) -> {
            ProblemCompleteMapper completions = sessions.getMapper(ProblemCompleteMapper.class);
            ProblemEventOutboxService outbox = mock(ProblemEventOutboxService.class);
            JudgeConfig config = new JudgeConfig();
            ProblemCompletionAwardService awards = new ProblemCompletionAwardService(completions,
                    outbox, config, Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC));
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                Future<Boolean> first = pool.submit(() -> awardTransaction(transactions, awards, ready, start));
                Future<Boolean> second = pool.submit(() -> awardTransaction(transactions, awards, ready, start));
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM problem_complete WHERE user_id=71 AND problem_id=81", Integer.class));
                verify(outbox, times(1)).recordPointAward(any());

                doThrow(new IllegalStateException("simulated outbox persistence failure"))
                        .when(outbox).recordPointAward(any());
                assertThrows(IllegalStateException.class, () -> transactions.execute(status ->
                        awards.completeOnAc(72L, 82L, BigDecimal.ONE)));
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM problem_complete WHERE user_id=72 AND problem_id=82", Integer.class));
                reset(outbox);
                assertEquals(Boolean.TRUE, transactions.execute(status ->
                        awards.completeOnAc(72L, 82L, BigDecimal.ONE)));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM problem_complete WHERE user_id=72 AND problem_id=82", Integer.class));
            } finally {
                pool.shutdownNow();
            }
        });
    }

    private boolean awardTransaction(TransactionTemplate transactions, ProblemCompletionAwardService service,
                                     CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("concurrency test start timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        return transactions.execute(status -> service.completeOnAc(71L, 81L, new BigDecimal("2.00")));
    }

    private void withMySql(MySqlScenario scenario) throws Exception {
        String url = System.getenv("OJ_T19_MYSQL_URL");
        Assumptions.assumeTrue(url != null && !url.trim().isEmpty(),
                "Set OJ_T19_MYSQL_URL to disposable loopback schema oj_t19_it");
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("OJ_T19_ALLOW_DESTRUCTIVE_FIXTURE")),
                "Set OJ_T19_ALLOW_DESTRUCTIVE_FIXTURE=true only for disposable test data");
        assertTrue(url.matches("(?i)^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):\\d+/oj_t19_it(?:[?].*)?$"),
                "Refusing destructive DDL outside loopback-only oj_t19_it schema");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(environmentOrDefault("OJ_T19_MYSQL_USER", "root"));
        dataSource.setPassword(environmentOrDefault("OJ_T19_MYSQL_PASSWORD", ""));
        createSchema(dataSource);
        try {
            MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            factoryBean.setConfiguration(configuration);
            GlobalConfig globalConfig = new GlobalConfig();
            globalConfig.setIdentifierGenerator(new DefaultIdentifierGenerator());
            factoryBean.setGlobalConfig(globalConfig);
            Resource[] mappers = new PathMatchingResourcePatternResolver().getResources(
                    "classpath*:mapper/{ContestRecordsService,ContestAnswerRecordsMapper,ProblemCompleteMapper}.xml");
            factoryBean.setMapperLocations(mappers);
            SqlSessionTemplate sessions = new SqlSessionTemplate(factoryBean.getObject());
            TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            scenario.run(sessions, transactions, new JdbcTemplate(dataSource));
        } finally {
            dropSchema(dataSource);
        }
    }

    private void createSchema(DataSource dataSource) throws Exception {
        dropSchema(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE contest_records(record_id BIGINT PRIMARY KEY,contest_id BIGINT NOT NULL,user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,status BOOLEAN NOT NULL,score DECIMAL(10,3),applied_attempt_seq BIGINT NOT NULL DEFAULT 0,applied_attempt_id BIGINT NULL,UNIQUE KEY uk_contest_user_problem(contest_id,user_id,problem_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_answer_records(record_id BIGINT PRIMARY KEY,answer JSON NULL) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_attempt(attempt_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
            statement.execute("CREATE TABLE problem_complete(user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,PRIMARY KEY(user_id,problem_id)) ENGINE=InnoDB");
        }
    }

    private void dropSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS contest_attempt");
            statement.execute("DROP TABLE IF EXISTS problem_complete");
            statement.execute("DROP TABLE IF EXISTS contest_answer_records");
            statement.execute("DROP TABLE IF EXISTS contest_records");
        }
    }

    private String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    @FunctionalInterface
    private interface MySqlScenario {
        void run(SqlSessionTemplate sessions, TransactionTemplate transactions, JdbcTemplate jdbc) throws Exception;
    }
}

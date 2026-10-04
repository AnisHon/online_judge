package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.commons.enumeration.ContestAuth;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.enumeration.ProblemType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ProblemEventOutboxMapper;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.mapper.SupplementContestMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.mapper.UserSubmitMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.anishan.problem.service.ContestProblemSnapshotService;
import com.anishan.problem.service.ProblemEventOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Real InnoDB two-connection races. Requires a disposable schema named oj_t18b_it. */
class ContestParticipationMysqlRaceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final Long FIRST_CONTEST = 991001L;
    private static final Long SECOND_CONTEST = 991002L;
    private static final Long USER_ID = 992001L;
    private static final Long PROBLEM_ID = 993001L;

    @Test
    void mysqlSerializesHandInAgainstAcceptanceAndRemovalAgainstAcceptedAttempt() throws Exception {
        String url = System.getenv("OJ_T18B_MYSQL_URL");
        Assumptions.assumeTrue(url != null && !url.trim().isEmpty(),
                "Set OJ_T18B_MYSQL_URL to a disposable MySQL schema named oj_t18b_it");
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("OJ_T18B_ALLOW_DESTRUCTIVE_FIXTURE")),
                "Set OJ_T18B_ALLOW_DESTRUCTIVE_FIXTURE=true only for a disposable test database");
        assertTrue(url.matches("(?i)^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):\\d+/oj_t18b_it(?:[?].*)?$"),
                "Refusing destructive test DDL outside a loopback-only oj_t18b_it schema");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(environmentOrDefault("OJ_T18B_MYSQL_USER", "root"));
        dataSource.setPassword(environmentOrDefault("OJ_T18B_MYSQL_PASSWORD", ""));

        createFixtureSchema(dataSource);
        try {
            runRaces(dataSource);
        } finally {
            dropFixtureSchema(dataSource);
        }
    }

    private void runRaces(DataSource dataSource) throws Exception {
        ContestMapper contestMapper;
        UserContestMapper userContestMapper;
        UserSubmitMapper userSubmitMapper;
        SupplementContestMapper supplementMapper;
        ContestAttemptMapper attemptMapper;
        SqlSessionTemplate template = createSqlSessionTemplate(dataSource);
        contestMapper = template.getMapper(ContestMapper.class);
        userContestMapper = template.getMapper(UserContestMapper.class);
        userSubmitMapper = template.getMapper(UserSubmitMapper.class);
        supplementMapper = template.getMapper(SupplementContestMapper.class);
        attemptMapper = template.getMapper(ContestAttemptMapper.class);

        seedActivity(dataSource, FIRST_CONTEST);
        seedActivity(dataSource, SECOND_CONTEST);
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        ContestParticipationService participation = new ContestParticipationService(contestMapper,
                userContestMapper, userSubmitMapper, supplementMapper, attemptMapper, CLOCK);

        ContestProblemSnapshotService snapshots = mock(ContestProblemSnapshotService.class);
        ContestProblemSnapshot frozenProblem = new ContestProblemSnapshot();
        frozenProblem.setContestId(SECOND_CONTEST);
        frozenProblem.setProblemId(PROBLEM_ID);
        frozenProblem.setProblemType(ProblemType.OJ.getValue());
        frozenProblem.setMaxScore(new java.math.BigDecimal("10.00"));
        when(snapshots.createIfAbsent(SECOND_CONTEST)).thenReturn(Collections.singletonList(frozenProblem));
        com.anishan.problem.service.SubmitLogService submitLogs = mock(com.anishan.problem.service.SubmitLogService.class);
        when(submitLogs.createQueued(any(JudgeInfo.class))).thenReturn(994001L);
        ProblemEventOutboxService outbox = mock(ProblemEventOutboxService.class);
        when(outbox.recordJudgeDispatch(any(JudgeInfo.class))).thenReturn("mysql-race-test-event");
        com.anishan.problem.mapper.ContestRecordsMapper contestRecords =
                mock(com.anishan.problem.mapper.ContestRecordsMapper.class);
        com.anishan.problem.domain.entity.ContestRecords answerProjection =
                new com.anishan.problem.domain.entity.ContestRecords();
        answerProjection.setRecordId(994002L);
        when(contestRecords.selectForUpdate(SECOND_CONTEST, USER_ID, PROBLEM_ID))
                .thenReturn(answerProjection);
        ContestSubmissionCoordinator coordinator = new ContestSubmissionCoordinator(contestMapper,
                userContestMapper, userSubmitMapper, supplementMapper, snapshots, attemptMapper,
                contestRecords,
                mock(com.anishan.problem.mapper.ContestAnswerRecordsMapper.class),
                mock(SubmitLogMapper.class), mock(ProblemEventOutboxMapper.class), submitLogs, outbox, CLOCK);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch handInWritten = new CountDownLatch(1);
            CountDownLatch allowHandInCommit = new CountDownLatch(1);
            Future<Boolean> handIn = pool.submit(() -> transactions.execute(status -> {
                boolean accepted = participation.handIn(FIRST_CONTEST, USER_ID);
                handInWritten.countDown();
                await(allowHandInCommit);
                return accepted;
            }));
            assertTrue(handInWritten.await(5, TimeUnit.SECONDS));

            CountDownLatch acceptanceStarted = new CountDownLatch(1);
            Future<?> acceptance = pool.submit(() -> {
                acceptanceStarted.countDown();
                return transactions.execute(status -> {
                    coordinator.acceptOjSubmission(judgeInfo(FIRST_CONTEST));
                    return null;
                });
            });
            assertTrue(acceptanceStarted.await(5, TimeUnit.SECONDS));
            Thread.sleep(200L);
            assertFalse(acceptance.isDone(), "OJ acceptance must wait for the hand-in contest-row lock");
            allowHandInCommit.countDown();
            assertTrue(handIn.get(5, TimeUnit.SECONDS));
            ExecutionException rejectedAcceptance = assertThrows(ExecutionException.class,
                    () -> acceptance.get(5, TimeUnit.SECONDS));
            assertInstanceOf(ApiStatusException.class, rejectedAcceptance.getCause());
            assertEquals(409, ((ApiStatusException) rejectedAcceptance.getCause()).getStatusCode());

            CountDownLatch attemptWritten = new CountDownLatch(1);
            CountDownLatch allowAttemptCommit = new CountDownLatch(1);
            Future<?> acceptedAttempt = pool.submit(() -> transactions.execute(status -> {
                coordinator.acceptOjSubmission(judgeInfo(SECOND_CONTEST));
                attemptWritten.countDown();
                await(allowAttemptCommit);
                return null;
            }));
            assertTrue(attemptWritten.await(5, TimeUnit.SECONDS));

            CountDownLatch removalStarted = new CountDownLatch(1);
            Future<?> removal = pool.submit(() -> {
                removalStarted.countDown();
                return transactions.execute(status -> participation.removeUsers(
                        SECOND_CONTEST, Collections.singletonList(USER_ID)));
            });
            assertTrue(removalStarted.await(5, TimeUnit.SECONDS));
            Thread.sleep(200L);
            assertFalse(removal.isDone(), "Roster removal must wait for accepted attempt commit");
            allowAttemptCommit.countDown();
            acceptedAttempt.get(5, TimeUnit.SECONDS);
            ExecutionException rejectedRemoval = assertThrows(ExecutionException.class,
                    () -> removal.get(5, TimeUnit.SECONDS));
            assertInstanceOf(ApiStatusException.class, rejectedRemoval.getCause());
            assertEquals(409, ((ApiStatusException) rejectedRemoval.getCause()).getStatusCode());

            assertEquals(1, scalar(dataSource,
                    "SELECT COUNT(*) FROM contest_attempt WHERE contest_id=" + SECOND_CONTEST));
            assertEquals(1, scalar(dataSource,
                    "SELECT COUNT(*) FROM user_contest WHERE contest_id=" + SECOND_CONTEST
                            + " AND user_id=" + USER_ID));
        } finally {
            pool.shutdownNow();
        }
    }

    private static SqlSessionTemplate createSqlSessionTemplate(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setIdentifierGenerator(new DefaultIdentifierGenerator());
        factoryBean.setGlobalConfig(globalConfig);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] mappers = resolver.getResources("classpath*:mapper/{ContestMapper,UserContestMapper,UserSubmitMapper,SupplementContestMapper,ContestAttemptMapper}.xml");
        factoryBean.setMapperLocations(mappers);
        return new SqlSessionTemplate(factoryBean.getObject());
    }

    private static void seedActivity(DataSource dataSource, Long contestId) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO contest (contest_id,user_id,title,list_id,description,auth,type,pwd,"
                    + "start_time,end_time,submitted,del_flag,create_time,update_time,scoring_version,next_attempt_seq,problem_snapshot_at) VALUES ("
                    + contestId + ",1,'race',1,'',0,0,'', '2026-10-03 09:00:00.000','2026-10-03 11:00:00.000',0,0,"
                    + "'2026-10-03 08:00:00.000','2026-10-03 08:00:00.000',2,0,NULL)");
            statement.executeUpdate("INSERT INTO user_contest(user_id,contest_id) VALUES ("
                    + USER_ID + "," + contestId + ")");
        }
    }

    private static void createFixtureSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS contest_attempt");
            statement.execute("DROP TABLE IF EXISTS supplement_contest");
            statement.execute("DROP TABLE IF EXISTS user_submit");
            statement.execute("DROP TABLE IF EXISTS user_contest");
            statement.execute("DROP TABLE IF EXISTS contest");
            statement.execute("CREATE TABLE contest (contest_id BIGINT PRIMARY KEY,user_id BIGINT,title VARCHAR(200),"
                    + "list_id BIGINT,description TEXT,auth INT,type INT,pwd VARCHAR(200),start_time DATETIME(3),"
                    + "end_time DATETIME(3),submitted TINYINT,del_flag TINYINT,create_time DATETIME(3),"
                    + "update_time DATETIME(3),scoring_version INT,next_attempt_seq BIGINT,problem_snapshot_at DATETIME(3)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE user_contest (user_id BIGINT NOT NULL,contest_id BIGINT NOT NULL,"
                    + "PRIMARY KEY(user_id,contest_id),KEY idx_user_contest_contest(contest_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE user_submit (user_id BIGINT NOT NULL,contest_id BIGINT NOT NULL,"
                    + "submit_time DATETIME(3) NOT NULL,PRIMARY KEY(user_id,contest_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE supplement_contest (user_id BIGINT NOT NULL,contest_id BIGINT NOT NULL,"
                    + "deadline DATETIME(3) NOT NULL,PRIMARY KEY(user_id,contest_id)) ENGINE=InnoDB");
            statement.execute("CREATE TABLE contest_attempt (attempt_id BIGINT NOT NULL PRIMARY KEY,contest_id BIGINT NOT NULL,"
                    + "user_id BIGINT NOT NULL,problem_id BIGINT NOT NULL,attempt_seq BIGINT NOT NULL,submit_id BIGINT NOT NULL,"
                    + "kind VARCHAR(16) NOT NULL,accepted_at DATETIME(3) NOT NULL,state VARCHAR(16) NOT NULL,"
                    + "judge_status VARCHAR(32) NOT NULL,score DECIMAL(10,2),correct BOOLEAN,completed_at DATETIME(3),"
                    + "UNIQUE KEY uk_test_attempt_seq(contest_id,attempt_seq),UNIQUE KEY uk_test_attempt_submit(submit_id),"
                    + "KEY idx_test_attempt_user(contest_id,user_id)) ENGINE=InnoDB");
        }
    }

    private static void dropFixtureSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS contest_attempt");
            statement.execute("DROP TABLE IF EXISTS supplement_contest");
            statement.execute("DROP TABLE IF EXISTS user_submit");
            statement.execute("DROP TABLE IF EXISTS user_contest");
            statement.execute("DROP TABLE IF EXISTS contest");
        }
    }

    private static int scalar(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); java.sql.ResultSet result = connection.createStatement().executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static JudgeInfo judgeInfo(Long contestId) {
        return new JudgeInfo().setUserId(USER_ID).setProblemId(PROBLEM_ID).setContestId(contestId)
                .setLanguageId(1L).setLanguage("Java").setCode("public class Main {}")
                .setTimeLimit(1000L).setMemoryLimit(256L).setStackLimit(128);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for transaction release");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding test transaction", interrupted);
        }
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}

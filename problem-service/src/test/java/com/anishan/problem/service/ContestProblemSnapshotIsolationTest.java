package com.anishan.problem.service;

import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.dto.ProblemListOrderBatchDto;
import com.anishan.problem.domain.dto.ProblemListOrderItemDto;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.ProblemStatistic;
import com.anishan.problem.domain.vo.UserScore;
import com.anishan.problem.domain.vo.UserStatistic;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestMutationReferenceMapper;
import com.anishan.problem.mapper.ContestProblemSnapshotMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import com.anishan.problem.mapper.ProblemListMapper;
import com.anishan.problem.mapper.ProblemMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import com.anishan.problem.mapper.RecordsMapper;
import com.anishan.problem.service.impl.ContestProblemSnapshotServiceImpl;
import com.anishan.problem.service.impl.ProblemListServiceImpl;
import com.anishan.problem.service.impl.ProblemProblemListServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.anishan.api.config.ConstConfig;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** MySQL-only transactional races use a disposable loopback schema and destructive opt-in. */
class ContestProblemSnapshotIsolationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);

    @Test
    void batchOrderStillLocksTheListAndUpdatesTheCompleteRoster() {
        ProblemListMapper listMapper = mock(ProblemListMapper.class);
        ProblemProblemListMapper relationMapper = mock(ProblemProblemListMapper.class);
        ProblemProblemListService relationService = mock(ProblemProblemListService.class);
        TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new MybatisConfiguration(), "snapshot-isolation-order"), ProblemProblemListRelation.class);
        LocalDateTime expected = NOW.minusMinutes(1);
        when(listMapper.selectUpdateTimeForUpdate(50L)).thenReturn(expected);
        when(listMapper.touchUpdateTime(50L, expected)).thenReturn(1);
        when(relationService.list(org.mockito.ArgumentMatchers.<Wrapper<ProblemProblemListRelation>>any()))
                .thenReturn(Arrays.asList(relation(50L, 101L), relation(50L, 102L)));

        ProblemListServiceImpl service = new ProblemListServiceImpl(relationService, listMapper,
                relationMapper, mock(ContestProblemSnapshotService.class), mock(ConstConfig.class));
        ProblemListOrderBatchDto request = new ProblemListOrderBatchDto();
        request.setListId(50L);
        request.setExpectedUpdateTime(expected);
        request.setItems(Arrays.asList(order(102L, 1), order(101L, 2)));

        assertTrue(service.updateProblemOrder(request));
        verify(listMapper).selectUpdateTimeForUpdate(50L);
        verify(relationMapper).updateProblemOrderBatch(50L, request.getItems());
        verify(listMapper).touchUpdateTime(50L, expected);
    }

    @Test
    void delByListIdsBuildsAListScopedDeleteAndLocksIdsInAscendingOrder() {
        ProblemProblemListMapper relationMapper = mock(ProblemProblemListMapper.class);
        ProblemListMapper listMapper = mock(ProblemListMapper.class);
        TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new MybatisConfiguration(), "snapshot-isolation-delete"), ProblemProblemListRelation.class);
        when(listMapper.selectUpdateTimeForUpdate(any())).thenReturn(NOW);
        CapturingProblemProblemListService service = new CapturingProblemProblemListService(relationMapper, listMapper);

        service.delByListIds(Arrays.asList(30L, 10L, 30L));

        verify(listMapper).selectUpdateTimeForUpdate(10L);
        verify(listMapper).selectUpdateTimeForUpdate(30L);
        assertNotNull(service.capturedDelete);
        String sql = service.capturedDelete.getSqlSegment().toLowerCase();
        assertTrue(sql.contains("list_id"), sql);
        assertTrue(sql.contains("in"), sql);
    }

    @Test
    void concurrentListEditCannotProduceAMixedFrozenRoster() throws Exception {
        withMySql((dataSource, sessions, transactions, jdbc) -> {
            seedActiveContest(dataSource, 7101L, 80L, 0, null);
            seedProblem(dataSource, 101L, "One");
            seedProblem(dataSource, 102L, "Two");
            seedProblem(dataSource, 103L, "Three");
            jdbc.update("insert into problem_problem_list(list_id,problem_id,problem_order,score) values (80,101,1,10),(80,102,2,20)");

            ContestProblemSnapshotServiceImpl snapshots = snapshots(sessions);
            ProblemListMapper lists = sessions.getMapper(ProblemListMapper.class);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CountDownLatch snapshotWritten = new CountDownLatch(1);
                CountDownLatch allowSnapshotCommit = new CountDownLatch(1);
                Future<?> snapshotTx = pool.submit(() -> transactions.execute(status -> {
                    List<ContestProblemSnapshot> rows = snapshots.createIfAbsent(7101L);
                    assertEquals(2, rows.size());
                    snapshotWritten.countDown();
                    await(allowSnapshotCommit);
                    return null;
                }));
                assertTrue(snapshotWritten.await(5, TimeUnit.SECONDS));

                CountDownLatch editStarted = new CountDownLatch(1);
                Future<?> listEdit = pool.submit(() -> transactions.execute(status -> {
                    editStarted.countDown();
                    LocalDateTime version = lists.selectUpdateTimeForUpdate(80L);
                    jdbc.update("delete from problem_problem_list where list_id=80 and problem_id=102");
                    jdbc.update("update problem_problem_list set problem_order=2,score=30 where list_id=80 and problem_id=101");
                    jdbc.update("insert into problem_problem_list(list_id,problem_id,problem_order,score) values (80,103,1,5)");
                    assertEquals(1, lists.touchUpdateTime(80L, version));
                    return null;
                }));
                assertTrue(editStarted.await(5, TimeUnit.SECONDS));
                Thread.sleep(200L);
                assertFalse(listEdit.isDone(), "A list edit must wait for the freeze transaction's list lock");
                allowSnapshotCommit.countDown();
                snapshotTx.get(5, TimeUnit.SECONDS);
                listEdit.get(5, TimeUnit.SECONDS);

                List<ContestProblemSnapshot> frozen = sessions.getMapper(ContestProblemSnapshotMapper.class)
                        .selectByContestId(7101L);
                assertEquals(Arrays.asList(101L, 102L), frozen.stream()
                        .map(ContestProblemSnapshot::getProblemId).collect(Collectors.toList()));
                assertEquals(new java.math.BigDecimal("10.000"), frozen.get(0).getMaxScore());
                assertEquals(new java.math.BigDecimal("20.000"), frozen.get(1).getMaxScore());
                assertEquals(2, jdbc.queryForObject("select count(*) from problem_problem_list where list_id=80", Integer.class));
            } finally {
                pool.shutdownNow();
            }
        });
    }

    @Test
    void activeCaseMutationWaitsForFreezeThenGetsRejectedAndHomeworkSupplementIsHonored() throws Exception {
        withMySql((dataSource, sessions, transactions, jdbc) -> {
            seedActiveContest(dataSource, 7201L, 80L, 0, null);
            seedProblem(dataSource, 201L, "Active");
            jdbc.update("insert into problem_problem_list(list_id,problem_id,problem_order,score) values (80,201,1,10)");
            jdbc.update("insert into oj_problem_case(case_id,problem_id,input,output,score,del_flag,create_time,update_time) values (301,201,'old.in','old.out',10,0,?,?)", NOW, NOW);

            ContestProblemSnapshotServiceImpl snapshots = snapshots(sessions);
            ContestMutationGuard guard = guard(sessions);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CountDownLatch snapshotWritten = new CountDownLatch(1);
                CountDownLatch allowSnapshotCommit = new CountDownLatch(1);
                Future<?> snapshotTx = pool.submit(() -> transactions.execute(status -> {
                    snapshots.createIfAbsent(7201L);
                    snapshotWritten.countDown();
                    await(allowSnapshotCommit);
                    return null;
                }));
                assertTrue(snapshotWritten.await(5, TimeUnit.SECONDS));

                CountDownLatch mutationStarted = new CountDownLatch(1);
                Future<?> mutationTx = pool.submit(() -> transactions.execute(status -> {
                    mutationStarted.countDown();
                    ContestMutationGuard.MutationContext context = guard.lockAndInspect(Collections.singletonList(201L));
                    guard.requireScoringMutable(context);
                    jdbc.update("update oj_problem_case set score=99 where case_id=301");
                    return null;
                }));
                assertTrue(mutationStarted.await(5, TimeUnit.SECONDS));
                Thread.sleep(200L);
                assertFalse(mutationTx.isDone(), "Scoring mutation must serialize on contest before problem/case locks");
                allowSnapshotCommit.countDown();
                snapshotTx.get(5, TimeUnit.SECONDS);
                ExecutionException rejected = assertThrows(ExecutionException.class,
                        () -> mutationTx.get(5, TimeUnit.SECONDS));
                assertInstanceOf(ApiStatusException.class, rejected.getCause());
                assertEquals(409, ((ApiStatusException) rejected.getCause()).getStatusCode());
                assertEquals(new java.math.BigDecimal("10.000"),
                        jdbc.queryForObject("select score from oj_problem_case where case_id=301", java.math.BigDecimal.class));

                // An ended homework remains busy until a joined user's valid supplement deadline.
                jdbc.update("update contest set type=1,start_time=?,end_time=? where contest_id=7201",
                        NOW.minusHours(2), NOW.minusMinutes(30));
                jdbc.update("insert into user_contest(user_id,contest_id) values (901,7201)");
                jdbc.update("insert into supplement_contest(user_id,contest_id,deadline) values (901,7201,?)",
                        NOW.plusHours(1));
                transactions.execute(status -> {
                    ContestMutationGuard.MutationContext context = guard.lockAndInspect(Collections.singletonList(201L));
                    assertThrows(ApiStatusException.class, () -> guard.requireScoringMutable(context));
                    return null;
                });
                jdbc.update("update supplement_contest set deadline=? where user_id=901 and contest_id=7201",
                        NOW.minusMinutes(1));
                transactions.execute(status -> {
                    ContestMutationGuard.MutationContext context = guard.lockAndInspect(Collections.singletonList(201L));
                    assertDoesNotThrow(() -> guard.requireScoringMutable(context));
                    return null;
                });
            } finally {
                pool.shutdownNow();
            }
        });
    }

    @Test
    void contestReadAndStatisticsUseFrozenRosterAndScopeScoresByContestAndUser() throws Exception {
        withMySql((dataSource, sessions, transactions, jdbc) -> {
            seedActiveContest(dataSource, 7301L, 80L, 0, null);
            seedProblem(dataSource, 301L, "Live title");
            seedProblem(dataSource, 302L, "Second live title");
            jdbc.update("insert into problem_problem_list(list_id,problem_id,problem_order,score) values (80,302,1,99)");
            jdbc.update("insert into contest_problem_snapshot(contest_id,problem_id,problem_order,title,problem_type,max_score,created_at) values "
                    + "(7301,301,1,'Frozen title',1,15,?),(7301,302,2,'Frozen second',1,25,?)", NOW, NOW);
            jdbc.update("insert into user_contest(user_id,contest_id) values (911,7301),(912,7301),(913,7302)");
            jdbc.update("insert into contest_records(record_id,contest_id,user_id,problem_id,status,score) values "
                    + "(1,7301,911,301,1,12),(2,7301,912,301,0,4),(3,7302,911,301,1,999)");

            List<ProblemInListVo> roster = sessions.getMapper(ProblemListMapper.class)
                    .selectContestSnapshotProblems(7301L, 911L);
            assertEquals(Arrays.asList(301L, 302L), roster.stream().map(ProblemInListVo::getProblemId).collect(Collectors.toList()));
            assertEquals("Frozen title", roster.get(0).getTitle());
            assertEquals("Live statement", roster.get(0).getDescription());
            assertEquals(new java.math.BigDecimal("15.000"), roster.get(0).getScore());
            assertEquals(0, new java.math.BigDecimal("12.00").compareTo(roster.get(0).getUserScore()));
            assertNull(roster.get(1).getUserScore());

            RecordsMapper records = sessions.getMapper(RecordsMapper.class);
            List<ProblemStatistic> problemStats = records.selectProblemStatistic(7301L);
            assertEquals(2, problemStats.size());
            assertEquals("Frozen title", problemStats.get(0).getTitle());
            assertEquals(new java.math.BigDecimal("15.000"), problemStats.get(0).getScore());
            assertEquals(2, records.selectUserStatistic(7301L).size());
            List<UserScore> userScores = records.selectUserScore(911L, 7301L);
            assertEquals(2, userScores.size());
            assertEquals("Frozen title", userScores.get(0).getTitle());
            assertEquals(0, new java.math.BigDecimal("12.00").compareTo(userScores.get(0).getScore()));
        });
    }

    private void withMySql(MySqlScenario scenario) throws Exception {
        String url = System.getenv("OJ_T18C_MYSQL_URL");
        Assumptions.assumeTrue(url != null && !url.trim().isEmpty(),
                "Set OJ_T18C_MYSQL_URL to a disposable loopback schema named oj_t18c_it");
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("OJ_T18C_ALLOW_DESTRUCTIVE_FIXTURE")),
                "Set OJ_T18C_ALLOW_DESTRUCTIVE_FIXTURE=true only for disposable test data");
        assertTrue(url.matches("(?i)^jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):\\d+/oj_t18c_it(?:[?].*)?$"),
                "Refusing destructive test DDL outside a loopback-only oj_t18c_it schema");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(environmentOrDefault("OJ_T18C_MYSQL_USER", "root"));
        dataSource.setPassword(environmentOrDefault("OJ_T18C_MYSQL_PASSWORD", ""));
        createFixtureSchema(dataSource);
        try {
            SqlSessionTemplate sessions = createSqlSessionTemplate(dataSource);
            DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
            scenario.run(dataSource, sessions, new TransactionTemplate(manager), new JdbcTemplate(dataSource));
        } finally {
            dropFixtureSchema(dataSource);
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
        Resource[] mappers = new PathMatchingResourcePatternResolver().getResources(
                "classpath*:mapper/{ContestMapper,ContestAttemptMapper,ContestProblemSnapshotMapper,"
                        + "ContestMutationReferenceMapper,ProblemListMapper,ProblemProblemListMapper,"
                        + "ProblemMapper,OjProblemCaseMapper,RecordsMapper}.xml");
        factoryBean.setMapperLocations(mappers);
        SqlSessionFactory factory = factoryBean.getObject();
        return new SqlSessionTemplate(factory);
    }

    private static ContestProblemSnapshotServiceImpl snapshots(SqlSessionTemplate sessions) {
        return new ContestProblemSnapshotServiceImpl(sessions.getMapper(ContestMapper.class),
                sessions.getMapper(ProblemListMapper.class), sessions.getMapper(ProblemProblemListMapper.class),
                sessions.getMapper(ProblemMapper.class), sessions.getMapper(ContestProblemSnapshotMapper.class),
                sessions.getMapper(ContestRankSnapshotMapper.class), sessions.getMapper(ContestAttemptMapper.class), CLOCK);
    }

    private static ContestMutationGuard guard(SqlSessionTemplate sessions) {
        return new ContestMutationGuard(sessions.getMapper(ContestMutationReferenceMapper.class),
                sessions.getMapper(ContestMapper.class), sessions.getMapper(ProblemMapper.class),
                sessions.getMapper(OjProblemCaseMapper.class), CLOCK);
    }

    private static void createFixtureSchema(DataSource dataSource) throws Exception {
        dropFixtureSchema(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE contest(contest_id BIGINT PRIMARY KEY,user_id BIGINT,title VARCHAR(255),list_id BIGINT,description TEXT,auth INT,type INT,pwd VARCHAR(255),start_time DATETIME(3),end_time DATETIME(3),submitted TINYINT,del_flag TINYINT,create_time DATETIME(3),update_time DATETIME(3),scoring_version INT,next_attempt_seq BIGINT,problem_snapshot_at DATETIME(3)) ENGINE=InnoDB");
            s.execute("CREATE TABLE problem_list(list_id BIGINT PRIMARY KEY,update_time DATETIME(3),del_flag TINYINT) ENGINE=InnoDB");
            s.execute("CREATE TABLE problem_problem_list(list_id BIGINT,problem_id BIGINT,problem_order INT,score DECIMAL(10,3),PRIMARY KEY(list_id,problem_id),KEY idx_ppl_problem(problem_id)) ENGINE=InnoDB");
            s.execute("CREATE TABLE problem(problem_id BIGINT PRIMARY KEY,title VARCHAR(255),type INT,source VARCHAR(255),description TEXT,hint TEXT,auth INT,del_flag TINYINT,create_time DATETIME(3),update_time DATETIME(3)) ENGINE=InnoDB");
            s.execute("CREATE TABLE contest_problem_snapshot(contest_id BIGINT,problem_id BIGINT,problem_order INT,title VARCHAR(255),problem_type INT,max_score DECIMAL(10,3),created_at DATETIME(3),PRIMARY KEY(contest_id,problem_id),UNIQUE KEY(contest_id,problem_order)) ENGINE=InnoDB");
            s.execute("CREATE TABLE contest_attempt(attempt_id BIGINT PRIMARY KEY,contest_id BIGINT,user_id BIGINT,problem_id BIGINT,attempt_seq BIGINT,submit_id BIGINT,kind VARCHAR(16),accepted_at DATETIME(3),state VARCHAR(16),judge_status VARCHAR(32),score DECIMAL(10,2),correct BOOLEAN,completed_at DATETIME(3),KEY idx_attempt_problem(contest_id,problem_id,state)) ENGINE=InnoDB");
            s.execute("CREATE TABLE contest_rank_snapshot(contest_id BIGINT PRIMARY KEY,state VARCHAR(16),version BIGINT) ENGINE=InnoDB");
            s.execute("CREATE TABLE contest_records(record_id BIGINT PRIMARY KEY,contest_id BIGINT,user_id BIGINT,problem_id BIGINT,status TINYINT,score DECIMAL(10,3),applied_attempt_seq BIGINT DEFAULT 0,applied_attempt_id BIGINT NULL,UNIQUE KEY uk_contest_user_problem(contest_id,user_id,problem_id)) ENGINE=InnoDB");
            s.execute("CREATE TABLE user_contest(user_id BIGINT,contest_id BIGINT,PRIMARY KEY(user_id,contest_id),KEY idx_uc_contest(contest_id)) ENGINE=InnoDB");
            s.execute("CREATE TABLE user_submit(user_id BIGINT,contest_id BIGINT,submit_time DATETIME(3),PRIMARY KEY(user_id,contest_id)) ENGINE=InnoDB");
            s.execute("CREATE TABLE supplement_contest(user_id BIGINT,contest_id BIGINT,deadline DATETIME(3),PRIMARY KEY(user_id,contest_id)) ENGINE=InnoDB");
            s.execute("CREATE TABLE oj_problem_case(case_id BIGINT PRIMARY KEY,problem_id BIGINT,input VARCHAR(500),output VARCHAR(500),score DECIMAL(10,3),del_flag TINYINT,create_time DATETIME(3),update_time DATETIME(3),KEY idx_case_problem(problem_id,case_id)) ENGINE=InnoDB");
        }
    }

    private static void dropFixtureSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement s = connection.createStatement()) {
            s.execute("DROP TABLE IF EXISTS oj_problem_case");
            s.execute("DROP TABLE IF EXISTS supplement_contest");
            s.execute("DROP TABLE IF EXISTS user_submit");
            s.execute("DROP TABLE IF EXISTS user_contest");
            s.execute("DROP TABLE IF EXISTS contest_records");
            s.execute("DROP TABLE IF EXISTS contest_rank_snapshot");
            s.execute("DROP TABLE IF EXISTS contest_attempt");
            s.execute("DROP TABLE IF EXISTS contest_problem_snapshot");
            s.execute("DROP TABLE IF EXISTS problem_problem_list");
            s.execute("DROP TABLE IF EXISTS problem");
            s.execute("DROP TABLE IF EXISTS problem_list");
            s.execute("DROP TABLE IF EXISTS contest");
        }
    }

    private static void seedActiveContest(DataSource dataSource, Long contestId, Long listId,
                                          int type, LocalDateTime unused) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement s = connection.createStatement()) {
            s.executeUpdate("INSERT INTO problem_list(list_id,update_time,del_flag) VALUES (" + listId
                    + ",'2026-10-03 08:00:00.000',0)");
            s.executeUpdate("INSERT INTO contest(contest_id,user_id,title,list_id,description,auth,type,pwd,start_time,end_time,"
                    + "submitted,del_flag,create_time,update_time,scoring_version,next_attempt_seq,problem_snapshot_at) VALUES ("
                    + contestId + ",1,'snapshot-test'," + listId + ",'',0," + type + ",'','2026-10-03 09:00:00.000',"
                    + "'2026-10-03 11:00:00.000',0,0,'2026-10-03 08:00:00.000','2026-10-03 08:00:00.000',2,0,NULL)");
        }
    }

    private static void seedProblem(DataSource dataSource, Long problemId, String title) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement s = connection.createStatement()) {
            s.executeUpdate("INSERT INTO problem(problem_id,title,type,source,description,hint,auth,del_flag,create_time,update_time) VALUES ("
                    + problemId + ",'" + title + "',1,'source','Live statement','hint',1,0,"
                    + "'2026-10-03 08:00:00.000','2026-10-03 08:00:00.000')");
        }
    }

    private static ProblemProblemListRelation relation(Long listId, Long problemId) {
        ProblemProblemListRelation relation = new ProblemProblemListRelation();
        relation.setListId(listId);
        relation.setProblemId(problemId);
        return relation;
    }

    private static ProblemListOrderItemDto order(Long problemId, Integer index) {
        ProblemListOrderItemDto item = new ProblemListOrderItemDto();
        item.setProblemId(problemId);
        item.setProblemOrder(index);
        return item;
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test latch");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test latch", e);
        }
    }

    @FunctionalInterface
    private interface MySqlScenario {
        void run(DataSource dataSource, SqlSessionTemplate sessions,
                 TransactionTemplate transactions, JdbcTemplate jdbc) throws Exception;
    }

    private static final class CapturingProblemProblemListService extends ProblemProblemListServiceImpl {
        private Wrapper<ProblemProblemListRelation> capturedDelete;

        private CapturingProblemProblemListService(ProblemProblemListMapper mapper, ProblemListMapper lists) {
            super(mapper, lists);
        }

        @Override
        public boolean remove(Wrapper<ProblemProblemListRelation> wrapper) {
            capturedDelete = wrapper;
            return true;
        }
    }
}

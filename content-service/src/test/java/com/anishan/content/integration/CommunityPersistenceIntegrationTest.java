package com.anishan.content.integration;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.integration.CommunityIntegrationSupport;
import com.anishan.content.config.CommunityWorkerProperties;
import com.anishan.content.mapper.ContentEventOutboxMapper;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionLikeMapper;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.service.CommentCreationService;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.content.service.SolutionLikeLocalWriteService;
import com.anishan.content.service.impl.ContentEventOutboxServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CommunityPersistenceIntegrationTest {
    private static final long SOLUTION_ID = 9000000000000000301L;
    private static final long OWNER_ID = 9000000000000000302L;
    private static final long ACTOR_ID = 9000000000000000303L;
    private static JdbcTemplate jdbc;
    private static SqlSessionTemplate sessions;
    private static TransactionTemplate transactions;
    private static SolutionLikeLocalWriteService likeService;
    private static ContentProblemReadVo publicProblem;

    @BeforeAll
    static void requireIsolatedMySql() throws Exception {
        CommunityIntegrationSupport.validateEnvironment();
        DataSource source = CommunityIntegrationSupport.dataSource("oj_it_content");
        jdbc = new JdbcTemplate(source);
        CommunityIntegrationSupport.sqlFile("resources/sql/migration/V20261004_4__content_solution_schema.sql");
        sessions = CommunityIntegrationSupport.sessions(source, "SolutionExplanationMapper", "SolutionLikeMapper", "SolutionCommentMapper", "ContentEventOutboxMapper");
        transactions = CommunityIntegrationSupport.transactions(source);
        ObjectMapper objectMapper = new ObjectMapper();
        CommunityWorkerProperties properties = new CommunityWorkerProperties();
        ContentEventOutboxServiceImpl outbox = new ContentEventOutboxServiceImpl(
                sessions.getMapper(ContentEventOutboxMapper.class), objectMapper,
                new Jackson2JsonMessageConverter(objectMapper), properties, Clock.systemUTC());
        likeService = new SolutionLikeLocalWriteService(sessions.getMapper(SolutionExplanationMapper.class),
                sessions.getMapper(SolutionLikeMapper.class), new SolutionAccessService(mock(
                com.anishan.api.client.problem.client.ProblemContentReadClient.class)), outbox, Clock.systemUTC());
        publicProblem = new ContentProblemReadVo("9000000000000000401", true, false, 1, true, true, "integration");
    }

    @Test
    void concurrentDuplicateLikesCommitOneRelationCounterAndOutboxEvent() throws Exception {
        clearSolutionFixtures();
        jdbc.update("INSERT INTO oj_it_content.solution_explanation(solution_id,title,problem_id,user_id,`private`,del_flag,moderation_state,comments_open,like_count,comment_count,`version`) VALUES(?,?,?,?,0,0,'NORMAL',1,0,0,0)",
                SOLUTION_ID, "community integration", 9000000000000000401L, OWNER_ID);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = pool.submit(() -> likeAfterBarrier(ready, start));
            Future<?> second = pool.submit(() -> likeAfterBarrier(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_content.solution_like WHERE solution_id=? AND user_id=? AND active=1", Integer.class, SOLUTION_ID, ACTOR_ID));
        assertEquals(1, jdbc.queryForObject("SELECT like_count FROM oj_it_content.solution_explanation WHERE solution_id=?", Integer.class, SOLUTION_ID));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_content.content_event_outbox WHERE dedupe_key=? AND event_type=?", Integer.class,
                "solution-liked:" + SOLUTION_ID + ":" + ACTOR_ID, CommunityEventType.SOLUTION_LIKED.name()));
    }

    @Test
    void redisCommentLimitUsesConfiguredTestPrefixAndFailsAtEleventhAttempt() {
        String prefix = CommunityIntegrationSupport.required("OJ_COMMUNITY_REDIS_PREFIX");
        org.springframework.data.redis.connection.RedisStandaloneConfiguration config =
                new org.springframework.data.redis.connection.RedisStandaloneConfiguration(
                        CommunityIntegrationSupport.required("OJ_COMMUNITY_REDIS_HOST"),
                        CommunityIntegrationSupport.port("OJ_COMMUNITY_REDIS_PORT"));
        org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory factory =
                new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        org.springframework.data.redis.core.StringRedisTemplate redis = new org.springframework.data.redis.core.StringRedisTemplate();
        redis.setConnectionFactory(factory);
        redis.setKeySerializer(new org.springframework.data.redis.serializer.RedisSerializer<String>() {
            @Override public byte[] serialize(String key) { return (prefix + key).getBytes(StandardCharsets.UTF_8); }
            @Override public String deserialize(byte[] bytes) {
                if (bytes == null) return null;
                String key = new String(bytes, StandardCharsets.UTF_8);
                return key.startsWith(prefix) ? key.substring(prefix.length()) : key;
            }
        });
        redis.afterPropertiesSet();
        long userId = 9000000000000000399L;
        String key = "content:comment:write:" + userId;
        redis.delete(key);
        try {
            com.anishan.content.service.CommentRateLimiter limiter = new com.anishan.content.service.CommentRateLimiter(redis);
            for (int attempt = 0; attempt < 10; attempt++) limiter.acquire(userId);
            com.anishan.commons.exception.ApiStatusException limited = assertThrows(
                    com.anishan.commons.exception.ApiStatusException.class, () -> limiter.acquire(userId));
            assertEquals(429, limited.getStatusCode());
            Long ttl = redis.getExpire(key);
            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 60);
            try (org.springframework.data.redis.connection.RedisConnection connection = factory.getConnection()) {
                assertEquals(1, connection.keys((prefix + key).getBytes(StandardCharsets.UTF_8)).size());
            }
        } finally {
            redis.delete(key);
            factory.destroy();
        }
    }

    @Test
    void commentAlreadyCreatedRemainsReadableWhenConcurrentCloseWinsBeforeNewWrite() throws Exception {
        clearSolutionFixtures();
        jdbc.update("INSERT INTO oj_it_content.solution_explanation(solution_id,title,problem_id,user_id,`private`,del_flag,moderation_state,comments_open,like_count,comment_count,`version`) VALUES(?,?,?,?,0,0,'NORMAL',1,0,0,0)",
                SOLUTION_ID, "comment integration", 9000000000000000401L, OWNER_ID);
        SolutionExplanationMapper solutionMapper = sessions.getMapper(SolutionExplanationMapper.class);
        SolutionCommentMapper commentMapper = sessions.getMapper(SolutionCommentMapper.class);
        CommentCreationService comments = new CommentCreationService(solutionMapper, commentMapper,
                new SolutionAccessService(mock(com.anishan.api.client.problem.client.ProblemContentReadClient.class)),
                new ContentEventOutboxServiceImpl(sessions.getMapper(ContentEventOutboxMapper.class), new ObjectMapper(),
                        new Jackson2JsonMessageConverter(new ObjectMapper()), new CommunityWorkerProperties(), Clock.systemUTC()),
                Clock.systemUTC());
        Long firstComment = transactions.execute(status -> comments.createRoot(ACTOR_ID, SOLUTION_ID, 0L,
                9000000000000000401L, publicProblem, "kept visible", "3b241101-e2bb-4255-8caf-4136c566a962").getCommentId());
        assertNotNull(firstComment);

        CountDownLatch closeOwnsRow = new CountDownLatch(1);
        CountDownLatch releaseClose = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> close = pool.submit(() -> transactions.execute(status -> {
                solutionMapper.selectCommentTargetForUpdate(SOLUTION_ID);
                closeOwnsRow.countDown();
                try {
                    if (!releaseClose.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("close barrier timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                solutionMapper.setCommentsOpen(SOLUTION_ID, false);
                return null;
            }));
            assertTrue(closeOwnsRow.await(5, TimeUnit.SECONDS));
            Future<Integer> lateComment = pool.submit(() -> {
                try {
                    transactions.execute(status -> comments.createRoot(ACTOR_ID + 1, SOLUTION_ID, 0L,
                            9000000000000000401L, publicProblem, "must not be inserted", "3b241101-e2bb-4255-8caf-4136c566a963"));
                    return 0;
                } catch (com.anishan.commons.exception.ApiStatusException exception) {
                    return exception.getStatusCode();
                }
            });
            releaseClose.countDown();
            close.get(10, TimeUnit.SECONDS);
            assertEquals(409, lateComment.get(10, TimeUnit.SECONDS));
        } finally {
            releaseClose.countDown();
            pool.shutdownNow();
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_content.solution_comment WHERE solution_id=? AND state='VISIBLE'", Integer.class, SOLUTION_ID));
        assertEquals(1, jdbc.queryForObject("SELECT comment_count FROM oj_it_content.solution_explanation WHERE solution_id=?", Integer.class, SOLUTION_ID));
    }

    private static void likeAfterBarrier(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("integration barrier timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        transactions.execute(status -> { likeService.setState(ACTOR_ID, SOLUTION_ID, true, publicProblem); return null; });
    }

    private static void clearSolutionFixtures() {
        jdbc.update("DELETE FROM oj_it_content.content_event_outbox WHERE dedupe_key LIKE ? OR dedupe_key LIKE ?",
                "solution-liked:" + SOLUTION_ID + ":%", "solution-commented:" + SOLUTION_ID + ":%");
        jdbc.update("DELETE FROM oj_it_content.comment_like WHERE comment_id IN " +
                "(SELECT comment_id FROM oj_it_content.solution_comment WHERE solution_id=?)", SOLUTION_ID);
        jdbc.update("DELETE FROM oj_it_content.solution_comment WHERE solution_id=?", SOLUTION_ID);
        jdbc.update("DELETE FROM oj_it_content.solution_like WHERE solution_id=?", SOLUTION_ID);
        jdbc.update("DELETE FROM oj_it_content.solution_moderation_action WHERE solution_id=?", SOLUTION_ID);
        jdbc.update("DELETE FROM oj_it_content.solution_explanation_content WHERE solution_id=?", SOLUTION_ID);
        jdbc.update("DELETE FROM oj_it_content.solution_explanation WHERE solution_id=?", SOLUTION_ID);
    }
}

package com.anishan.user.integration;

import com.anishan.api.integration.CommunityIntegrationSupport;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.user.domain.vo.FollowSummaryVo;
import com.anishan.user.mapper.UserFollowMapper;
import com.anishan.user.service.SysUserService;
import com.anishan.user.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

class UserSocialMigrationIntegrationTest {
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void requireIsolatedMySql() {
        CommunityIntegrationSupport.validateEnvironment();
        jdbc = CommunityIntegrationSupport.jdbc("oj_it_user");
    }

    @Test
    void socialMigrationIsRepeatableAndKeepsUniqueConstraints() throws Exception {
        // Only these fixture-owned tables are removed; the database/schema is never dropped.
        jdbc.execute("DROP TABLE IF EXISTS oj_it_user.user_point_award_receipt");
        jdbc.execute("DROP TABLE IF EXISTS oj_it_user.notification_fanout_job");
        jdbc.execute("DROP TABLE IF EXISTS oj_it_user.user_notification");
        jdbc.execute("DROP TABLE IF EXISTS oj_it_user.user_follow");
        String fixture = new String(Files.readAllBytes(CommunityIntegrationSupport.root()
                .resolve("user-service/src/test/resources/fixtures/user-social-base.sql")), StandardCharsets.UTF_8);
        jdbc.execute(fixture);
        jdbc.update("INSERT INTO oj_it_user.sys_user(user_id,status,del_flag) VALUES(9000000000000000101,0,0),(9000000000000000102,0,0) ON DUPLICATE KEY UPDATE status=0,del_flag=0");

        CommunityIntegrationSupport.sqlFile("resources/sql/migration/V20261004_1__user_social.sql");
        long firstCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='oj_it_user' AND table_name IN ('user_follow','user_notification','notification_fanout_job','user_point_award_receipt')", Long.class);
        CommunityIntegrationSupport.sqlFile("resources/sql/migration/V20261004_1__user_social.sql");
        long secondCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='oj_it_user' AND table_name IN ('user_follow','user_notification','notification_fanout_job','user_point_award_receipt')", Long.class);
        assertEquals(4, firstCount);
        assertEquals(firstCount, secondCount);

        jdbc.update("INSERT INTO oj_it_user.user_follow(follower_id,followee_id,created_at) VALUES(9000000000000000101,9000000000000000102,NOW(3))");
        assertThrows(RuntimeException.class, () -> jdbc.update("INSERT INTO oj_it_user.user_follow(follower_id,followee_id,created_at) VALUES(9000000000000000101,9000000000000000102,NOW(3))"));
        assertThrows(RuntimeException.class, () -> jdbc.update("INSERT INTO oj_it_user.user_follow(follower_id,followee_id,created_at) VALUES(9000000000000000101,9000000000000000101,NOW(3))"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_user.user_follow", Integer.class));
    }

    @Test
    void concurrentFollowRequestsUseTheCompositeKeyAsTheIdempotencyBoundary() throws Exception {
        CommunityIntegrationSupport.sqlFile("resources/sql/migration/V20261004_1__user_social.sql");
        long followerId = 9000000000000000111L;
        long followeeId = 9000000000000000112L;
        jdbc.update("INSERT INTO oj_it_user.sys_user(user_id,status,del_flag) VALUES(?,0,0),(?,0,0) " +
                "ON DUPLICATE KEY UPDATE status=0,del_flag=0", followerId, followeeId);
        jdbc.update("DELETE FROM oj_it_user.user_follow WHERE follower_id=? AND followee_id=?", followerId, followeeId);

        SysUserService userService = mock(SysUserService.class);
        when(userService.isFollowableUser(followeeId)).thenReturn(true);
        FollowServiceImpl followService = new FollowServiceImpl(
                CommunityIntegrationSupport.sessions(CommunityIntegrationSupport.dataSource("oj_it_user"), "UserFollowMapper")
                        .getMapper(UserFollowMapper.class), userService);
        TransactionTemplate tx = CommunityIntegrationSupport.transactions(CommunityIntegrationSupport.dataSource("oj_it_user"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<FollowSummaryVo> first = pool.submit(() -> followAs(followService, tx, followerId, followeeId, ready, start));
            Future<FollowSummaryVo> second = pool.submit(() -> followAs(followService, tx, followerId, followeeId, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS).isFollowing());
            assertTrue(second.get(10, TimeUnit.SECONDS).isFollowing());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_user.user_follow WHERE follower_id=? AND followee_id=?",
                Integer.class, followerId, followeeId));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_user.user_follow WHERE followee_id=?",
                Long.class, followeeId));
    }

    private static FollowSummaryVo followAs(FollowServiceImpl followService, TransactionTemplate tx,
                                            long followerId, long followeeId,
                                            CountDownLatch ready, CountDownLatch start) {
        LoginUser loginUser = new LoginUser();
        SysUser user = new SysUser();
        user.setUserId(followerId);
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("follow race barrier timed out");
            return tx.execute(status -> followService.follow(followeeId));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

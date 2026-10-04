package com.anishan.user.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSocialMapperTest {

    private final Configuration configuration = new Configuration();

    @BeforeEach
    void parseMapperXml() {
        parse("mapper/UserFollowMapper.xml");
        parse("mapper/SysUserMapper.xml");
        parse("mapper/UserNotificationMapper.xml");
        parse("mapper/NotificationFanoutJobMapper.xml");
        parse("mapper/UserPointAwardReceiptMapper.xml");
    }

    @Test
    void allSocialMapperStatementsAreRegistered() {
        assertMapped("com.anishan.user.mapper.UserFollowMapper.insert");
        assertMapped("com.anishan.user.mapper.UserFollowMapper.deleteByFollowerAndFollowee");
        assertMapped("com.anishan.user.mapper.UserFollowMapper.countFollowing");
        assertMapped("com.anishan.user.mapper.UserFollowMapper.countFollowers");
        assertMapped("com.anishan.user.mapper.UserFollowMapper.exists");
        assertMapped("com.anishan.user.mapper.UserFollowMapper.selectFollowingPage");
        assertMapped("com.anishan.user.mapper.UserFollowMapper.selectFollowersPage");
        assertMapped("com.anishan.user.mapper.SysUserMapper.countVisibleUser");
        assertMapped("com.anishan.user.mapper.SysUserMapper.countFollowableUser");
        assertMapped("com.anishan.user.mapper.SysUserMapper.selectUserSummaryRowsByIds");
        assertMapped("com.anishan.user.mapper.UserNotificationMapper.insertNotificationRow");
        assertMapped("com.anishan.user.mapper.UserNotificationMapper.selectPageByRecipient");
        assertMapped("com.anishan.user.mapper.UserNotificationMapper.countUnread");
        assertMapped("com.anishan.user.mapper.UserNotificationMapper.markRead");
        assertMapped("com.anishan.user.mapper.UserNotificationMapper.markAllRead");
        assertMapped("com.anishan.user.mapper.NotificationFanoutJobMapper.insertIfAbsent");
        assertMapped("com.anishan.user.mapper.NotificationFanoutJobMapper.selectClaimable");
        assertMapped("com.anishan.user.mapper.NotificationFanoutJobMapper.markClaimed");
        assertMapped("com.anishan.user.mapper.UserPointAwardReceiptMapper.insertReceipt");
        assertMapped("com.anishan.user.mapper.UserPointAwardReceiptMapper.selectByUserAndProblem");
        assertMapped("com.anishan.user.mapper.UserPointAwardReceiptMapper.selectByEventId");
    }

    @Test
    void notificationReadsAndUpdatesAlwaysIncludeRecipientScope() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("notificationId", 99L);
        parameters.put("recipientId", 42L);
        parameters.put("throughId", 100L);
        parameters.put("readAt", "2026-10-03 12:00:00.000");
        parameters.put("unreadOnly", false);
        parameters.put("offset", 0L);
        parameters.put("pageSize", 20);

        String pageSql = boundSql("UserNotificationMapper.selectPageByRecipient", parameters);
        String markReadSql = boundSql("UserNotificationMapper.markRead", parameters);
        String markAllSql = boundSql("UserNotificationMapper.markAllRead", parameters);
        String unreadCountSql = boundSql("UserNotificationMapper.countUnread", parameters);

        assertTrue(pageSql.contains("where recipient_id = ?"), pageSql);
        assertTrue(markReadSql.contains("where notification_id = ? and recipient_id = ?"), markReadSql);
        assertTrue(markAllSql.contains("where recipient_id = ? and notification_id <= ?"), markAllSql);
        assertTrue(unreadCountSql.contains("where recipient_id = ? and read_at is null"), unreadCountSql);
        assertFalse(markReadSql.contains("where notification_id = ? and read_at is null"), markReadSql);
    }

    @Test
    void fanoutClaimUsesAClaimableStateAndSkipLocked() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("now", "2026-10-03 12:00:00.000");
        parameters.put("limit", 20);
        String sql = boundSql("NotificationFanoutJobMapper.selectClaimable", parameters);

        assertTrue(sql.contains("status = 'pending'"), sql);
        assertTrue(sql.contains("status = 'sending' and lease_until <= ?"), sql);
        assertTrue(sql.endsWith("for update skip locked"), sql);

        String updateSql = boundSql("NotificationFanoutJobMapper.markClaimed", parameters);
        assertTrue(updateSql.contains("where event_id = ? and next_attempt_at <= ?"), updateSql);
        assertTrue(updateSql.contains("status = 'sending' and lease_until <= ?"), updateSql);
    }

    @Test
    void followQueriesBindIdsAndUseStableDeletedUserFilteredPagination() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("followerId", 2098765432109876543L);
        parameters.put("followeeId", 2098765432109876543L);
        parameters.put("offset", 40L);
        parameters.put("pageSize", 20);

        com.anishan.user.domain.entity.UserFollow follow = new com.anishan.user.domain.entity.UserFollow();
        follow.setFollowerId(100L);
        follow.setFolloweeId(200L);
        follow.setCreatedAt(java.time.LocalDateTime.parse("2026-10-03T12:00:00"));
        String insertSql = boundSql("UserFollowMapper.insert", Map.of("follow", follow));
        String pageSql = boundSql("UserFollowMapper.selectFollowingPage", parameters);
        String followersSql = boundSql("UserFollowMapper.selectFollowersPage", parameters);
        String countSql = boundSql("UserFollowMapper.countFollowers", parameters);
        String summarySql = boundSql("SysUserMapper.selectUserSummaryRowsByIds", Map.of("userIds", java.util.List.of(100L, 200L)));

        assertTrue(insertSql.startsWith("insert into user_follow"), insertSql);
        assertFalse(insertSql.contains("on duplicate key"), insertSql);
        assertTrue(pageSql.contains("f.follower_id = ?"), pageSql);
        assertTrue(pageSql.contains("u.del_flag = 0"), pageSql);
        assertTrue(pageSql.contains("order by f.created_at desc, f.followee_id desc limit ?, ?"), pageSql);
        assertTrue(followersSql.contains("order by f.created_at desc, f.follower_id desc limit ?, ?"), followersSql);
        assertTrue(countSql.contains("f.followee_id = ?"), countSql);
        assertTrue(countSql.contains("u.del_flag = 0"), countSql);
        assertTrue(summarySql.contains("su.user_id in ( ? , ? )"), summarySql);
        assertTrue(summarySql.contains("sr.special_role = 1"), summarySql);
        assertTrue(summarySql.contains("su.del_flag = 0"), summarySql);
    }

    private void parse(String resource) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        } catch (Exception exception) {
            throw new AssertionError("Cannot parse " + resource, exception);
        }
    }

    private void assertMapped(String statementId) {
        assertTrue(configuration.hasStatement(statementId), statementId);
        MappedStatement statement = configuration.getMappedStatement(statementId);
        assertNotNull(statement);
    }

    private String boundSql(String statementId, Map<String, Object> parameters) {
        BoundSql sql = configuration.getMappedStatement("com.anishan.user.mapper." + statementId)
                .getBoundSql(parameters);
        return sql.getSql().replaceAll("\\s+", " ").trim().toLowerCase();
    }
}

package com.anishan.user.integration;

import com.anishan.api.event.CommunityEvent;
import com.anishan.api.event.CommunityEventType;
import com.anishan.api.integration.CommunityIntegrationSupport;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.mapper.UserNotificationMapper;
import com.anishan.user.service.impl.NotificationDeliveryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP;
import org.mybatis.spring.SqlSessionTemplate;
import javax.sql.DataSource;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NotificationIntegrationTest {
    private static JdbcTemplate jdbc;
    private static SqlSessionTemplate sessions;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void requireIsolatedMySql() throws Exception {
        CommunityIntegrationSupport.validateEnvironment();
        DataSource source = CommunityIntegrationSupport.dataSource("oj_it_user");
        jdbc = new JdbcTemplate(source);
        jdbc.execute(new String(Files.readAllBytes(CommunityIntegrationSupport.root()
                .resolve("user-service/src/test/resources/fixtures/user-social-base.sql")), StandardCharsets.UTF_8));
        CommunityIntegrationSupport.sqlFile("resources/sql/migration/V20261004_1__user_social.sql");
        sessions = CommunityIntegrationSupport.sessions(source, "SysUserMapper", "UserNotificationMapper");
        transactions = CommunityIntegrationSupport.transactions(source);
    }

    @Test
    void duplicateDeliveryDoesNotDuplicateOrResetReadState() {
        jdbc.update("DELETE FROM oj_it_user.user_notification WHERE recipient_id IN (9000000000000000201,9000000000000000202)");
        jdbc.update("INSERT INTO oj_it_user.sys_user(user_id,status,del_flag) VALUES(9000000000000000201,0,0),(9000000000000000202,0,0) ON DUPLICATE KEY UPDATE status=0,del_flag=0");
        CommunityEvent event = new CommunityEvent().setSchemaVersion(1).setEventId("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
                .setEventType(CommunityEventType.SOLUTION_LIKED).setDedupeKey("solution-liked:301:9000000000000000201")
                .setOccurredAt(OffsetDateTime.now().toString()).setActorId("9000000000000000201").setSolutionId("301")
                .setRecipientIds(Collections.singletonList("9000000000000000202"));
        ObjectMapper objectMapper = new ObjectMapper();
        UserNotificationMapper notificationMapper = sessions.getMapper(UserNotificationMapper.class);
        SysUserMapper sysUserMapper = sessions.getMapper(SysUserMapper.class);
        NotificationDeliveryServiceImpl delivery = new NotificationDeliveryServiceImpl(notificationMapper, sysUserMapper, objectMapper, Clock.systemUTC());
        transactions.execute(status -> { delivery.deliver(event); return null; });
        Long id = jdbc.queryForObject("SELECT notification_id FROM oj_it_user.user_notification WHERE recipient_id=9000000000000000202 AND dedupe_key=?", Long.class, event.getDedupeKey());
        jdbc.update("UPDATE oj_it_user.user_notification SET read_at=NOW(3) WHERE notification_id=?", id);
        transactions.execute(status -> { delivery.deliver(event); return null; });
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_user.user_notification WHERE recipient_id=9000000000000000202 AND dedupe_key=?", Integer.class, event.getDedupeKey()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oj_it_user.user_notification WHERE recipient_id=9000000000000000202 AND dedupe_key=? AND read_at IS NOT NULL", Integer.class, event.getDedupeKey()));
    }

    @Test
    void rabbitTopologyConfirmsRoutesReturnsUnroutableAndDeadLettersRetryExhaustion() throws Exception {
        CachingConnectionFactory factory = new CachingConnectionFactory(
                CommunityIntegrationSupport.required("OJ_COMMUNITY_RABBIT_HOST"),
                CommunityIntegrationSupport.port("OJ_COMMUNITY_RABBIT_PORT"));
        factory.setUsername(CommunityIntegrationSupport.required("OJ_COMMUNITY_RABBIT_USER"));
        factory.setPassword(CommunityIntegrationSupport.required("OJ_COMMUNITY_RABBIT_PASSWORD"));
        factory.setVirtualHost(CommunityIntegrationSupport.VHOST);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        RabbitAdmin admin = new RabbitAdmin(factory);
        for (Declarable declarable : new com.anishan.user.config.CommunityTopologyConfig().communityEventTopology().getDeclarables()) {
            if (declarable instanceof Exchange) admin.declareExchange((Exchange) declarable);
            else if (declarable instanceof Queue) admin.declareQueue((Queue) declarable);
            else if (declarable instanceof Binding) admin.declareBinding((Binding) declarable);
        }
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMandatory(true);
        CountDownLatch returned = new CountDownLatch(1);
        template.setReturnsCallback(message -> returned.countDown());

        String eventId = UUID.randomUUID().toString();
        CorrelationData routed = new CorrelationData(eventId);
        template.convertAndSend(com.anishan.api.event.FixedEventRoutes.COMMUNITY_EXCHANGE,
                "solution.liked.v1", "community-it", message -> {
                    message.getMessageProperties().setMessageId(eventId);
                    return message;
                }, routed);
        assertTrue(routed.getFuture().get(5, TimeUnit.SECONDS).isAck());
        Message delivered = template.receive(com.anishan.api.event.FixedEventRoutes.COMMUNITY_QUEUE, 5000);
        assertNotNull(delivered);
        assertEquals(eventId, delivered.getMessageProperties().getMessageId());

        CorrelationData unroutable = new CorrelationData(UUID.randomUUID().toString());
        template.convertAndSend(com.anishan.api.event.FixedEventRoutes.COMMUNITY_EXCHANGE,
                "unknown.route", "unroutable", unroutable);
        assertTrue(unroutable.getFuture().get(5, TimeUnit.SECONDS).isAck());
        assertTrue(returned.await(5, TimeUnit.SECONDS));

        DirectExchange deadExchange = new DirectExchange("community-it-retry-dlx", true, false);
        Queue deadQueue = QueueBuilder.durable("community-it-retry-dlq").build();
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", deadExchange.getName());
        args.put("x-dead-letter-routing-key", "failed");
        Queue retryQueue = new Queue("community-it-retry-source", true, false, false, args);
        admin.declareExchange(deadExchange);
        admin.declareQueue(deadQueue);
        admin.declareQueue(retryQueue);
        admin.declareBinding(BindingBuilder.bind(deadQueue).to(deadExchange).with("failed"));
        String originalId = UUID.randomUUID().toString();
        template.convertAndSend("", retryQueue.getName(), "retry-me", message -> {
            message.getMessageProperties().setMessageId(originalId);
            return message;
        });
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch exhausted = new CountDownLatch(1);
        org.springframework.amqp.rabbit.connection.Connection springConnection = factory.createConnection();
        Channel channel = springConnection.createChannel(false);
        String tag = channel.basicConsume(retryQueue.getName(), false, new DefaultConsumer(channel) {
            @Override public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws java.io.IOException {
                if (attempts.incrementAndGet() < 3) channel.basicNack(envelope.getDeliveryTag(), false, true);
                else {
                    channel.basicNack(envelope.getDeliveryTag(), false, false);
                    exhausted.countDown();
                }
            }
        });
        try {
            assertTrue(exhausted.await(5, TimeUnit.SECONDS));
            Message dead = template.receive(deadQueue.getName(), 5000);
            assertNotNull(dead);
            assertEquals(originalId, dead.getMessageProperties().getMessageId());
            assertEquals(3, attempts.get());
        } finally {
            channel.basicCancel(tag);
            channel.close();
            springConnection.close();
            admin.deleteQueue(retryQueue.getName());
            admin.deleteQueue(deadQueue.getName());
            admin.deleteExchange(deadExchange.getName());
            factory.destroy();
        }
    }
}

package com.anishan.content.service.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;
import com.anishan.commons.enumeration.SseEvent;
import com.anishan.content.service.SseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    private final ObjectMapper objectMapper;

    private static final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    private static final Map<Long, ConcurrentHashSet<String>> userTokenMap = new ConcurrentHashMap<>();

    public synchronized void saveUserSession(Long userId, String token) {
        if (!userTokenMap.containsKey(userId)) {
             userTokenMap.put(userId, new ConcurrentHashSet<>());
        }

        userTokenMap.get(userId).add(token);
    }

    public synchronized void removeUserSession(Long userId, String token) {
        if (userTokenMap.containsKey(userId)) {
            userTokenMap.get(userId).remove(token);
        }
    }

    private Set<String> getUserSessions(Long userId) {
        return userTokenMap.get(userId);
    }


    @Override
    public SseEmitter reconnect(String uuid) {
        if (uuid == null) {
            return null;
        }
        return sseEmitterMap.get(uuid);

    }

    /**
     * 创建连接
     */
    @Override
    public SseEmitter createSse(String uuid, Long userId) {
        if (uuid == null) {
            return null;
        }

        // session与用户Id关联
        saveUserSession(userId, uuid);

        SseEmitter sseEmitter = new SseEmitter(0L);

        //完成后回调
        sseEmitter.onCompletion(() -> {
            sseEmitterMap.remove(uuid);
            removeUserSession(userId, uuid);
            log.info("[{}]销毁sse连接", uuid);
        });
        //异常回调
        sseEmitter.onError(
                throwable -> {
                    try {
                        log.info("[{}]连接异常,{}", uuid, throwable.toString());
                        sseEmitter.send(SseEmitter.event()
                                .id(uuid.toString())
                                .name("发生异常！")
                                .data("发生异常请重试！")
                                .reconnectTime(3000));
                        sseEmitterMap.put(uuid, sseEmitter);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
        );

        try {
            sseEmitter.send(SseEmitter.event().reconnectTime(5000));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        sseEmitterMap.put(uuid, sseEmitter);
        log.info("[{}]创建sse连接成功！", uuid);



        return sseEmitter;
    }

    /**
     * 给指定用户发送消息
     *
     */
    @Override
    public boolean sendMessage(String uuid, String messageId, String message) {
        if (uuid == null) {
            return false;
        }
        if (StrUtil.isBlank(message)) {
            log.info("参数异常id: [{}]，msg为null", uuid);
            return false;
        }
        SseEmitter sseEmitter = sseEmitterMap.get(uuid);
        if (sseEmitter == null) {
            log.info("消息推送失败id:[{}],没有创建连接，请重试。", uuid);
            return false;
        }
        try {
            sseEmitter.send(SseEmitter.event().id(messageId).reconnectTime(30 * 1000L).data(message));
            return true;
        }catch (Exception e) {
            sseEmitterMap.remove(uuid);
            log.info("用户{},消息id:{},推送异常:{}", uuid,messageId, e.getMessage());
            sseEmitter.complete();
            return false;
        }
    }

    /**
     * 断开
     *
     * @param uuid session ID
     * @param userId 用户Id
     */
    @Override
    public void closeSse(String uuid, Long userId){
        if (sseEmitterMap.containsKey(uuid)) {
            SseEmitter sseEmitter = sseEmitterMap.get(uuid);
            sseEmitter.complete();
        }
    }

    @Override
    public boolean sendPlainString(String uuid, SseEvent sseEvent, String message) {
        return sendMessage(uuid, sseEvent.getEvent(), message);
    }

    /**
     * 发送SSE消息，不进行对象类型转换
     * @param userId 用户ID
     * @param sseEvent 事件类型
     * @param message 消息
     */
    @Override
    public void sendPlainString(Long userId, SseEvent sseEvent, String message) {
        Set<String> userSessions = getUserSessions(userId);
        for (String userSession : userSessions) {
            sendMessage(userSession, sseEvent.getEvent(), message);
        }
    }

    @Override
    public void sendMessage(String uuid, SseEvent sseEvent, Object message) {
        try {
            String s = objectMapper.writeValueAsString(message);
            sendMessage(uuid, sseEvent.getEvent(), s);
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败", e);
        }
    }

    /**
     * 向某个User广播消息
     * @param userId 用户ID
     * @param sseEvent sse消息事件
     * @param message 消息
     */
    @Override
    public void sendMessage(Long userId, SseEvent sseEvent, Object message) {
        Set<String> userSessions = getUserSessions(userId);
        for (String userSession : userSessions) {
            sendMessage(userSession, sseEvent, message);
        }
    }


    /**
     * 全体广播消息
     * @param sseEvent 消息事件
     * @param message 消息
     */
    @Override
    public void sendMessage(SseEvent sseEvent, Object message) {
        for (String s : sseEmitterMap.keySet()) {
            sendMessage(s, sseEvent, message);
        }
    }


    @Override
    public synchronized void close(String uuid) {
        if (sseEmitterMap.containsKey(uuid)) {
            SseEmitter sseEmitter = sseEmitterMap.get(uuid);
            if (sseEmitter != null) {
                log.info("{} 关闭了一个链接", uuid);
                sseEmitter.complete();
            }
        }

    }


    @Scheduled(cron = "0/30 * * * * ? ")
    private void heartbeat() {
        log.info("定时清理");

        ArrayList<Long> delete = new ArrayList<>();
        for (Long userId : userTokenMap.keySet()) {
            ConcurrentHashSet<String> value = userTokenMap.get(userId);
            if (value == null || value.isEmpty()) {
                delete.add(userId);
                continue;
            }

            for (String s : value) {
                sendPlainString(s, SseEvent.Ping, "30000");
            }
        }
        delete.forEach(userTokenMap::remove);
    }
}
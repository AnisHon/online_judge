package com.anishan.user.util;

import cn.hutool.core.util.StrUtil;
import com.anishan.commons.e.SseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseUtils {

    private final ObjectMapper objectMapper;


    private static final Map<Long, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    /**
     * 创建连接
     */
    public SseEmitter createSse(Long userId) {
        if (userId == null) {
            return null;
        }

        //默认30秒超时,设置为0L则永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        //完成后回调
        sseEmitter.onCompletion(() -> sseEmitterMap.remove(userId));

        //异常回调
        sseEmitter.onError(
                throwable -> {
                    try {
                        log.info("[{}]连接异常,{}", userId, throwable.toString());
                        sseEmitter.send(SseEmitter.event()
                                .id(userId.toString())
                                .name("发生异常！")
                                .data("发生异常请重试！")
                                .reconnectTime(3000));
                        sseEmitterMap.put(userId, sseEmitter);
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
        sseEmitterMap.put(userId, sseEmitter);
        log.info("[{}]创建sse连接成功！", userId);
        return sseEmitter;
    }

    /**
     * 给指定用户发送消息
     *
     */
    public boolean sendMessage(Long id,String messageId, String message) {
        if (StrUtil.isBlank(message)) {
            log.info("参数异常id: [{}]，msg为null", id);
            return false;
        }
        SseEmitter sseEmitter = sseEmitterMap.get(id);
        if (sseEmitter == null) {
            log.info("消息推送失败id:[{}],没有创建连接，请重试。", id);
            return false;
        }
        try {
            sseEmitter.send(SseEmitter.event().id(messageId).reconnectTime(60 * 1000L).data(message));
            return true;
        }catch (Exception e) {
            sseEmitterMap.remove(id);
            log.info("用户{},消息id:{},推送异常:{}", id,messageId, e.getMessage());
            sseEmitter.complete();
            return false;
        }
    }

    /**
     * 断开
     * @param id 用户ID
     */
    public void closeSse(Long id){
        if (sseEmitterMap.containsKey(id)) {
            SseEmitter sseEmitter = sseEmitterMap.get(id);
            sseEmitter.complete();
            sseEmitterMap.remove(id);
        }
    }

    public boolean sendMessage(Long id, SseEvent sseEvent, Object message) {
        try {
            String s = objectMapper.writeValueAsString(message);
            return sendMessage(id, sseEvent.getEvent(), s);
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败", e);
            return false;
        }
    }

}
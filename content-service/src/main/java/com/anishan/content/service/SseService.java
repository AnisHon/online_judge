package com.anishan.content.service;

import com.anishan.commons.enumeration.SseEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    long count();

    SseEmitter reconnect(String uuid);

    SseEmitter createSse(String uuid, Long userId);

    boolean sendMessage(String uuid, String messageId, String message);

    boolean sendPlainString(String uuid, SseEvent sseEvent, String message);

    void sendPlainString(Long userId, SseEvent sseEvent, String message);

    void sendMessage(String uuid, SseEvent sseEvent, Object message);

//    void sendMessage(Long userId, SseEvent sseEvent, Object message);
//
//    void sendMessage(SseEvent sseEvent, Object message);

    void close(String uuid);
}

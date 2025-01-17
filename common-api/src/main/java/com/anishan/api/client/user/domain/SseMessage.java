package com.anishan.api.client.user.domain;


import com.anishan.commons.enumeration.SseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class SseMessage {

    /**
     * uuid和userid二选一，userId是广播的
     */
    private String uuid;
    private Long userId;
    private SseEvent event;
    private String data;


    public static SseMessage create(String uuid, final SseEvent event, final Object data) {
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        SseMessage sseMessage = new SseMessage();
        sseMessage.uuid = uuid;
        sseMessage.event = event;
        sseMessage.data = json;
        return sseMessage;
    }

    public static SseMessage create(Long userId, final SseEvent event, final Object data) {
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        SseMessage sseMessage = new SseMessage();
        sseMessage.userId = userId;
        sseMessage.event = event;
        sseMessage.data = json;
        return sseMessage;
    }



}

package com.anishan.api.client.user.domain;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.anishan.commons.enumeration.SseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class SseMessage {

    private String uuid;
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



}

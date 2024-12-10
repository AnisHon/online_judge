package com.anishan.commons.e;

import lombok.Getter;

@Getter
public enum SseEvent {
    UpdatePoint("update-point"),
    ;


    private final String event;

    SseEvent(String event) {
        this.event = event;
    }

}

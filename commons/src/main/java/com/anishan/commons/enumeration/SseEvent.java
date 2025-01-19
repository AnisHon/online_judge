package com.anishan.commons.enumeration;

import lombok.Getter;

@Getter
public enum SseEvent {
    UPDATE_POINT("update-point"),
    PING("retry"),
    UPDATE_JUDGE_STATE("update-judge-state"),
    SET_UUID("set-uuid"),
    ;


    private final String event;

    SseEvent(String event) {
        this.event = event;
    }

}

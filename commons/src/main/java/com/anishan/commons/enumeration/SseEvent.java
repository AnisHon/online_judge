package com.anishan.commons.enumeration;

import lombok.Getter;

@Getter
public enum SseEvent {
    UpdatePoint("update-point"),
    Ping("retry"),
    UpdateJudgeState("update-judge-state"),
    SetUUID("set-uuid"),
    ;


    private final String event;

    SseEvent(String event) {
        this.event = event;
    }

}

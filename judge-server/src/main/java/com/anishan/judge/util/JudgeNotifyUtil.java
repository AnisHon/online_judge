package com.anishan.judge.util;

import com.anishan.api.client.content.client.ContentInternalClient;
import com.anishan.api.client.user.domain.SseMessage;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.enumeration.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class JudgeNotifyUtil {

    private final ContentInternalClient contentInternalClient;

    public void notifyCompiling(String uuid) {
        notify(uuid, JudgeResult.Compiling, "");
    }

    public void notifyRunning(String uuid) {
        notify(uuid, JudgeResult.Running, "");
    }

    public void notify(String uuid, JudgeResult result, String stderr) {
        notify(uuid, result, "", stderr);
    }

    /**
     * 需要注意一下顺序问题，这里stdin是第三个
     */
    public void notify(String uuid, JudgeResult result, String stdout, String stderr) {
        HashMap<String, String> map = new HashMap<>();
        map.put("state", result.value());
        map.put("stderr", stderr);
        map.put("stdout", stdout);

        SseMessage sseMessage = SseMessage.create(uuid, SseEvent.UpdateJudgeState, map);
        contentInternalClient.sendMessage(sseMessage);
    }

}

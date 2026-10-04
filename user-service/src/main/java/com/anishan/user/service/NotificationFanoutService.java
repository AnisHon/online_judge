package com.anishan.user.service;

import com.anishan.api.event.CommunityEvent;
import com.anishan.user.domain.entity.NotificationFanoutJob;

public interface NotificationFanoutService {

    void register(CommunityEvent event);

    NotificationFanoutJob claimOne(String leaseOwner);

    void processOneBatch(NotificationFanoutJob claimedJob);

    boolean markFailed(NotificationFanoutJob claimedJob, String errorCode);

    void replayFailed(String eventId);
}

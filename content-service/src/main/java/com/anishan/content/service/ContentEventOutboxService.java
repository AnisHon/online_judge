package com.anishan.content.service;

import com.anishan.api.event.CommunityEvent;
import com.anishan.content.domain.entity.ContentEventOutbox;

import java.util.List;

/** Content-owned transactional outbox for the fixed community event protocol only. */
public interface ContentEventOutboxService {

    String recordCommunity(CommunityEvent event);

    List<ContentEventOutbox> claimBatch(String leaseOwner, int limit);

    boolean markSent(String eventId, String leaseOwner);

    boolean markFailed(ContentEventOutbox event, String leaseOwner, String errorCode);

    List<ContentEventOutbox> findUnsupportedDueEvents(int limit);

    int cleanupSentBatch();

    String replayCommunityFailed(String eventId);
}

package com.anishan.problem.service;

import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.event.PointAwardEvent;
import com.anishan.problem.domain.entity.ProblemEventOutbox;

import java.util.List;

/** Problem-domain outbox boundary. It intentionally has no community-event operation. */
public interface ProblemEventOutboxService {

    String POINTS_AWARDED = "POINTS_AWARDED";
    String JUDGE_DISPATCH = "JUDGE_DISPATCH";
    int MAX_JUDGE_PAYLOAD_BYTES = 600 * 1024;

    String recordPointAward(PointAwardEvent event);

    String recordJudgeDispatch(JudgeInfo judgeInfo);

    List<ProblemEventOutbox> claimBatch(String leaseOwner, int limit);

    boolean markSent(String eventId, String leaseOwner);

    boolean markFailed(ProblemEventOutbox claimedEvent, String leaseOwner, String errorCode);

    int cleanupSentBatch();

    List<ProblemEventOutbox> findUnsupportedDueEvents(int limit);
}

package com.anishan.problem.service;

import org.junit.jupiter.api.Test;

/** Runs the existing real-MySQL judge callback race/failure scenarios against oj_it_problem. */
class JudgeResultRaceIntegrationTest {
    @Test
    void duplicateAndOutOfOrderCallbacksRemainIdempotent() throws Exception {
        JudgeResultApplicationMysqlTest scenarios = new JudgeResultApplicationMysqlTest();
        scenarios.concurrentDuplicateCallbacksApplyCasesProjectionAndPointsOnlyOnce();
        scenarios.laterAttemptRemainsTheProjectionWhenItsEarlierAcCallbackArrivesLate();
        scenarios.failureAfterCaseAndProjectionWritesRollsBackAllRowsAndCanBeRetried();
    }
}

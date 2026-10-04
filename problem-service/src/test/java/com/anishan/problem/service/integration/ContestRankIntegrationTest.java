package com.anishan.problem.service;

import org.junit.jupiter.api.Test;

/** Reuses the existing MySQL 8 candidate/lease/race assertions inside the T24 schema allowlist. */
class ContestRankIntegrationTest {
    @Test
    void rankBuildLeaseTiePendingAndRecoveryScenariosUseIsolatedMysql() throws Exception {
        ContestFinalRankRaceTest scenarios = new ContestFinalRankRaceTest();
        scenarios.competingWorkersPublishOneVersionWithStableTiesAndRosterZeros();
        scenarios.pendingJudgeWaitsThenErrorsWithoutInventingAResultAndLateResultCanPublish();
        scenarios.expiredOwnerCannotPublishOrReuseItsCandidateVersion();
        scenarios.rebuildFailureKeepsOldPublishedVersionAndLegacyUsesExistingProjection();
    }
}

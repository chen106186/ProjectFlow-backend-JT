package com.jitong.projectflow.requirement.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequirementStatusPolicyTest {
    private final RequirementStatusPolicy policy = new RequirementStatusPolicy();

    @Test
    void pendingRequirementCanBeAccepted() {
        assertThat(policy.canTransition(RequirementStatus.PENDING_REVIEW, RequirementStatus.ACCEPTED)).isTrue();
    }

    @Test
    void acceptedRequirementCannotMoveBackToPending() {
        assertThat(policy.canTransition(RequirementStatus.ACCEPTED, RequirementStatus.PENDING_REVIEW)).isFalse();
    }

    @Test
    void pendingRequirementCanBeRejected() {
        assertThat(policy.canTransition(RequirementStatus.PENDING_REVIEW, RequirementStatus.REJECTED)).isTrue();
    }

    @Test
    void acceptedRequirementCannotBeRejected() {
        assertThat(policy.canTransition(RequirementStatus.ACCEPTED, RequirementStatus.REJECTED)).isFalse();
    }

    @Test
    void rejectedRequirementCannotMoveBackToPending() {
        assertThat(policy.canTransition(RequirementStatus.REJECTED, RequirementStatus.PENDING_REVIEW)).isFalse();
    }

    @Test
    void sameStatusTransitionIsAllowed() {
        assertThat(policy.canTransition(RequirementStatus.PENDING_REVIEW, RequirementStatus.PENDING_REVIEW)).isTrue();
    }

    @Test
    void pendingReviewCanBeShelvedAndRestoredButDecisionsRemainTerminal() {
        assertThat(policy.canTransition(RequirementStatus.PENDING_REVIEW, RequirementStatus.SHELVED)).isTrue();
        assertThat(policy.canTransition(RequirementStatus.SHELVED, RequirementStatus.PENDING_REVIEW)).isTrue();
        assertThat(policy.canTransition(RequirementStatus.SHELVED, RequirementStatus.ACCEPTED)).isFalse();
        assertThat(policy.canTransition(RequirementStatus.ACCEPTED, RequirementStatus.SHELVED)).isFalse();
        assertThat(policy.canTransition(RequirementStatus.REJECTED, RequirementStatus.SHELVED)).isFalse();
    }
}

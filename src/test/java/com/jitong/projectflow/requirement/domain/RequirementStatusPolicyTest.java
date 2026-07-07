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
}

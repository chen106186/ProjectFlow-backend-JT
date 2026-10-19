package com.jitong.projectflow.requirement.domain;

public class RequirementStatusPolicy {

    public boolean canTransition(RequirementStatus from, RequirementStatus to) {
        if (from == to) return true;
        if (from == RequirementStatus.SHELVED) return to == RequirementStatus.PENDING_REVIEW;
        return from == RequirementStatus.PENDING_REVIEW
                && (to == RequirementStatus.ACCEPTED || to == RequirementStatus.REJECTED || to == RequirementStatus.SHELVED);
    }
}

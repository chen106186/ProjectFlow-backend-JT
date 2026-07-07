package com.jitong.projectflow.requirement.domain;

public class RequirementStatusPolicy {

    public boolean canTransition(RequirementStatus from, RequirementStatus to) {
        if (from == to) return true;
        return from == RequirementStatus.PENDING_REVIEW
                && (to == RequirementStatus.ACCEPTED || to == RequirementStatus.REJECTED);
    }
}

CREATE TABLE pf_release_stage (
    release_id VARCHAR(36) NOT NULL,
    stage_code VARCHAR(32) NOT NULL,
    owner_id BIGINT NULL,
    planned_start DATE NULL,
    planned_end DATE NULL,
    actual_start DATE NULL,
    actual_end DATE NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
    conclusion TEXT NULL,
    completed_by BIGINT NULL,
    completed_at DATETIME NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (release_id, stage_code),
    INDEX idx_release_stage_owner (owner_id, status)
);

CREATE TABLE pf_release_record (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    release_id VARCHAR(36) NOT NULL,
    stage_code VARCHAR(32) NOT NULL,
    record_type VARCHAR(24) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    evidence_url VARCHAR(1000) NULL,
    before_json TEXT NULL,
    after_json TEXT NULL,
    actor_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_release_record_stage_time (release_id, stage_code, created_at),
    INDEX idx_release_record_actor (actor_id, created_at)
);

CREATE TABLE pf_release_application (
    release_id VARCHAR(36) NOT NULL PRIMARY KEY,
    environment VARCHAR(120) NOT NULL,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    change_summary TEXT NOT NULL,
    impact_scope TEXT NOT NULL,
    rollback_plan TEXT NOT NULL,
    monitoring_plan TEXT NOT NULL,
    support_plan TEXT NOT NULL,
    applicant_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME NULL,
    decision_at DATETIME NULL,
    decision_note TEXT NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_release_application_approver (approver_id, status)
);

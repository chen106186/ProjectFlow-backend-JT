ALTER TABLE pf_task ADD COLUMN work_type VARCHAR(24) NOT NULL DEFAULT 'GENERAL';

CREATE TABLE pf_release_execution (
    task_id BIGINT NOT NULL PRIMARY KEY,
    release_id VARCHAR(36) NOT NULL,
    environment VARCHAR(120) NOT NULL,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    lead_id BIGINT NOT NULL,
    operator_ids TEXT NOT NULL,
    verifier_id BIGINT NULL,
    support_ids TEXT NULL,
    meeting_planned_at DATETIME NULL,
    plan_revision INT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_release_execution_release (release_id)
);

CREATE TABLE pf_release_meeting (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    held_at DATETIME NOT NULL,
    chair_id BIGINT NOT NULL,
    attendee_ids TEXT NOT NULL,
    agenda TEXT NOT NULL,
    decisions TEXT NOT NULL,
    risks TEXT NOT NULL,
    action_items TEXT NOT NULL,
    conclusion VARCHAR(16) NOT NULL,
    plan_revision INT NOT NULL,
    recorded_by BIGINT NOT NULL,
    recorded_at DATETIME NOT NULL,
    INDEX idx_release_meeting_task (task_id, recorded_at)
);

CREATE TABLE pf_release_meeting_ack (
    meeting_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    confirmed_at DATETIME NOT NULL,
    PRIMARY KEY (meeting_id,user_id)
);

CREATE TABLE pf_release_report (
    task_id BIGINT NOT NULL PRIMARY KEY,
    actual_start DATETIME NOT NULL,
    actual_end DATETIME NOT NULL,
    package_ref VARCHAR(500) NOT NULL,
    execution_steps TEXT NOT NULL,
    smoke_result TEXT NOT NULL,
    monitoring_result TEXT NOT NULL,
    incident_notes TEXT NOT NULL,
    rolled_back TINYINT(1) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    residual_issues TEXT NOT NULL,
    verifier_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
    verified_by BIGINT NULL,
    verified_at DATETIME NULL,
    verification_note TEXT NULL,
    submitted_by BIGINT NOT NULL,
    submitted_at DATETIME NOT NULL
);

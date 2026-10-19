CREATE TABLE pf_requirement_task (
    requirement_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    PRIMARY KEY (requirement_id, task_id),
    INDEX idx_requirement_task_task (task_id)
);

CREATE TABLE pf_requirement_acceptance (
    requirement_id BIGINT NOT NULL PRIMARY KEY,
    result VARCHAR(16) NOT NULL,
    conclusion TEXT NULL,
    accepted_by BIGINT NULL,
    accepted_at DATETIME NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE pf_task_blocker (
    task_id BIGINT NOT NULL PRIMARY KEY,
    reason TEXT NULL,
    reported_by BIGINT NULL,
    reported_at DATETIME NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE pf_work_comment (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    business_type VARCHAR(16) NOT NULL,
    business_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_work_comment_business (business_type, business_id, created_at)
);

CREATE TABLE pf_delivery_release (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    acceptance_note TEXT NULL,
    released_at DATETIME NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_delivery_release_project (project_id, target_date)
);

CREATE TABLE pf_delivery_release_item (
    release_id VARCHAR(36) NOT NULL,
    business_type VARCHAR(16) NOT NULL,
    business_id BIGINT NOT NULL,
    PRIMARY KEY (release_id, business_type, business_id),
    INDEX idx_release_item_business (business_type, business_id)
);

CREATE TABLE pf_saved_work_view (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    filters_json TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_saved_work_view_user (user_id, scope)
);

CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    department_id BIGINT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    job_no VARCHAR(64) NULL,
    position_name VARCHAR(64) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    hire_date DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    path VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_menu_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY,
    module VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    operation_type VARCHAR(64) NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    before_value JSON NULL,
    after_value JSON NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sys_operation_log_business (business_type, business_id),
    INDEX idx_sys_operation_log_operator_time (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_project (
    id BIGINT PRIMARY KEY,
    project_type VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    stage VARCHAR(64) NULL,
    status VARCHAR(64) NOT NULL,
    contract_status VARCHAR(64) NULL,
    manager_id BIGINT NULL,
    description VARCHAR(2000) NULL,
    planned_start_date DATE NULL,
    planned_end_date DATE NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_project_type_status (project_type, status),
    INDEX idx_pf_project_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_project_node (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    node_name VARCHAR(200) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    planned_start_date DATE NULL,
    planned_end_date DATE NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    status VARCHAR(64) NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_project_node_project (project_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_task (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    role_name VARCHAR(64) NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assignee_id BIGINT NOT NULL,
    planned_start_date DATE NULL,
    planned_end_date DATE NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    description VARCHAR(2000) NULL,
    tags VARCHAR(500) NULL,
    remark VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_task_project (project_id),
    INDEX idx_pf_task_assignee_status (assignee_id, status),
    INDEX idx_pf_task_deadline (planned_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_requirement (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    requirement_type VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    description VARCHAR(2000) NULL,
    tags VARCHAR(500) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_requirement_creator_status (created_by, status),
    INDEX idx_pf_requirement_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_bug (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    creator_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    description VARCHAR(2000) NOT NULL,
    reproduce_steps VARCHAR(2000) NOT NULL,
    closed_at DATETIME NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_bug_project (project_id),
    INDEX idx_pf_bug_creator_assignee (creator_id, assignee_id),
    INDEX idx_pf_bug_status_priority (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_bug_comment (
    id BIGINT PRIMARY KEY,
    bug_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_bug_comment_bug_time (bug_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_file (
    id BIGINT PRIMARY KEY,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NULL,
    file_size BIGINT NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    storage_type VARCHAR(32) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    uploader_id BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_file_business (business_type, business_id),
    INDEX idx_pf_file_uploader_time (uploader_id, uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_notice (
    id BIGINT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    notice_type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    business_type VARCHAR(64) NULL,
    business_id BIGINT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_notice_receiver_read (receiver_id, read_flag),
    INDEX idx_pf_notice_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

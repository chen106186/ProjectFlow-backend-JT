CREATE TABLE pf_daily_report (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_daily_report_project_date (project_id, report_date),
    INDEX idx_pf_daily_report_reporter_date (reporter_id, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_project_report (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    report_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    planned_date DATE NOT NULL,
    actual_date DATE NULL,
    target_audience VARCHAR(200) NOT NULL,
    location_method VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_project_report_project_status (project_id, status),
    INDEX idx_pf_project_report_planned_date (planned_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_project_report_item (
    id BIGINT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    owner_id BIGINT NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    planned_date DATE NULL,
    description VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_project_report_item_report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000801, NULL, 'daily-report', 'Daily Report Management', 'MENU', '/daily-reports', 150, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'daily-report');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000802, (SELECT id FROM sys_menu WHERE code = 'daily-report'), 'daily-report:create', 'Create Daily Reports', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'daily-report:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000803, (SELECT id FROM sys_menu WHERE code = 'daily-report'), 'daily-report:update', 'Update Daily Reports', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'daily-report:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000811, NULL, 'project-report', 'Project Report Management', 'MENU', '/project-reports', 160, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project-report');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000812, (SELECT id FROM sys_menu WHERE code = 'project-report'), 'project-report:create', 'Create Project Reports', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project-report:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000813, (SELECT id FROM sys_menu WHERE code = 'project-report'), 'project-report:update', 'Update Project Reports', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project-report:update');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE m.code IN (
    'daily-report',
    'daily-report:create',
    'daily-report:update',
    'project-report',
    'project-report:create',
    'project-report:update'
)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
  );

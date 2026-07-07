CREATE INDEX idx_pf_project_name ON pf_project (name);
CREATE INDEX idx_pf_project_contract_status ON pf_project (contract_status);

CREATE INDEX idx_pf_task_status_priority ON pf_task (status, priority);
CREATE INDEX idx_pf_task_created_time ON pf_task (created_at);

CREATE INDEX idx_pf_bug_assignee_status ON pf_bug (assignee_id, status);
CREATE INDEX idx_pf_bug_created_time ON pf_bug (created_at);

CREATE INDEX idx_sys_user_department_enabled ON sys_user (department_id, enabled);

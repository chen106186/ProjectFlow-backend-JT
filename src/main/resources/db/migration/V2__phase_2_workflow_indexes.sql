CREATE INDEX idx_pf_requirement_status_priority ON pf_requirement (status, priority);
CREATE INDEX idx_pf_requirement_project_status ON pf_requirement (project_id, status);
CREATE INDEX idx_pf_requirement_created_time ON pf_requirement (created_by, created_at);

CREATE INDEX idx_pf_project_node_status_dates ON pf_project_node (project_id, status, planned_end_date);
CREATE INDEX idx_pf_project_node_parent ON pf_project_node (project_id, parent_id);

CREATE INDEX idx_pf_notice_receiver_time ON pf_notice (receiver_id, created_at);
CREATE INDEX idx_pf_notice_type_read ON pf_notice (notice_type, read_flag);

CREATE INDEX idx_sys_operation_log_module_time ON sys_operation_log (module, created_at);
CREATE INDEX idx_sys_operation_log_type_time ON sys_operation_log (operation_type, created_at);

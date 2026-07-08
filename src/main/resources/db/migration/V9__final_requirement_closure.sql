ALTER TABLE pf_task
    ADD COLUMN parent_id BIGINT NULL AFTER project_id,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER remark,
    ADD INDEX idx_pf_task_parent (parent_id),
    ADD INDEX idx_pf_task_project_parent_order (project_id, parent_id, sort_order);

ALTER TABLE pf_file
    ADD COLUMN storage_location VARCHAR(64) NOT NULL DEFAULT 'BUSINESS' AFTER version_no,
    ADD COLUMN file_category VARCHAR(64) NULL AFTER storage_location,
    ADD INDEX idx_pf_file_category (business_type, business_id, file_category);

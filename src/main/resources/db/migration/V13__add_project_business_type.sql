ALTER TABLE pf_project
    ADD COLUMN project_business_type VARCHAR(64) NULL COMMENT '项目业务类型' AFTER project_type;

CREATE INDEX idx_pf_project_business_type ON pf_project (project_business_type);
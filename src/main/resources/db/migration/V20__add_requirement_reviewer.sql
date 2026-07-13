ALTER TABLE pf_requirement ADD COLUMN reviewer_id BIGINT NULL AFTER project_id;
CREATE INDEX idx_pf_requirement_reviewer_status ON pf_requirement (reviewer_id, status);

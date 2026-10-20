ALTER TABLE pf_release_record
    ADD COLUMN file_id BIGINT NULL AFTER evidence_url,
    ADD INDEX idx_release_record_file (file_id);

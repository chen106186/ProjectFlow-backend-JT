CREATE TABLE IF NOT EXISTS pf_bug_task (
    bug_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bug_id, task_id),
    INDEX idx_pf_bug_task_task (task_id),
    INDEX idx_pf_bug_task_bug (bug_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO pf_bug_task (bug_id, task_id)
SELECT id, task_id
FROM pf_bug
WHERE task_id IS NOT NULL
  AND deleted = 0;

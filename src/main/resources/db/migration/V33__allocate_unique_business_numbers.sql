-- Preserve the earliest row for each existing number. Repair any historical
-- duplicates and NULLs above the current maximum before adding constraints.
CREATE TEMPORARY TABLE pf_bug_number_repair AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS replacement_offset
FROM (
    SELECT id, bug_no, ROW_NUMBER() OVER (PARTITION BY bug_no ORDER BY id) AS duplicate_rank
    FROM pf_bug
) ranked
WHERE bug_no IS NULL OR duplicate_rank > 1;

SET @bug_max_no := (SELECT COALESCE(MAX(bug_no), 0) FROM pf_bug);
UPDATE pf_bug b JOIN pf_bug_number_repair r ON r.id = b.id
SET b.bug_no = @bug_max_no + r.replacement_offset;
DROP TEMPORARY TABLE pf_bug_number_repair;

CREATE TEMPORARY TABLE pf_requirement_number_repair AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS replacement_offset
FROM (
    SELECT id, requirement_no,
           ROW_NUMBER() OVER (PARTITION BY requirement_no ORDER BY id) AS duplicate_rank
    FROM pf_requirement
) ranked
WHERE requirement_no IS NULL OR duplicate_rank > 1;

SET @requirement_max_no := (SELECT COALESCE(MAX(requirement_no), 0) FROM pf_requirement);
UPDATE pf_requirement r JOIN pf_requirement_number_repair repair ON repair.id = r.id
SET r.requirement_no = @requirement_max_no + repair.replacement_offset;
DROP TEMPORARY TABLE pf_requirement_number_repair;

ALTER TABLE pf_bug MODIFY bug_no BIGINT NOT NULL,
    ADD UNIQUE KEY uk_pf_bug_no (bug_no);
ALTER TABLE pf_requirement MODIFY requirement_no BIGINT NOT NULL,
    ADD UNIQUE KEY uk_pf_requirement_no (requirement_no);

CREATE TABLE pf_business_number (
    business_type VARCHAR(32) PRIMARY KEY,
    next_value BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pf_business_number (business_type, next_value)
SELECT 'BUG', COALESCE(MAX(bug_no), 0) + 1 FROM pf_bug;
INSERT INTO pf_business_number (business_type, next_value)
SELECT 'REQUIREMENT', COALESCE(MAX(requirement_no), 0) + 1 FROM pf_requirement;

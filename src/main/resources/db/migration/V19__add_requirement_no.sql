ALTER TABLE pf_requirement ADD COLUMN requirement_no BIGINT NULL AFTER id;
SET @reqno := 0;
UPDATE pf_requirement SET requirement_no = (@reqno := @reqno + 1) ORDER BY id;

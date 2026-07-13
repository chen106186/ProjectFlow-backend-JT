ALTER TABLE pf_bug ADD COLUMN bug_no BIGINT NULL AFTER id;

SET @bugno := 0;
UPDATE pf_bug SET bug_no = (@bugno := @bugno + 1) ORDER BY id;

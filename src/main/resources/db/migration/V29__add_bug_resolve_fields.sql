ALTER TABLE pf_bug
    ADD COLUMN solution       VARCHAR(50)  NULL COMMENT '解决方案（枚举值）' AFTER fix_detail,
    ADD COLUMN resolved_date  DATE         NULL COMMENT '解决日期'             AFTER solution,
    ADD COLUMN resolve_remark LONGTEXT     NULL COMMENT '解决备注（富文本）'   AFTER resolved_date;

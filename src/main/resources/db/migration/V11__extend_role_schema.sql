ALTER TABLE sys_role
    ADD COLUMN description VARCHAR(255) NULL COMMENT '描述' AFTER name,
    ADD COLUMN enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用' AFTER description,
    ADD COLUMN sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序' AFTER enabled;

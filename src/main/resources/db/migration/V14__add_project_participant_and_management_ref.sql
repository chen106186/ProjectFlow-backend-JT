-- 执行类项目关联管理类项目
ALTER TABLE pf_project
    ADD COLUMN management_project_id BIGINT NULL COMMENT '关联管理类项目ID' AFTER manager_id;

-- 项目参与人员关联表
CREATE TABLE pf_project_participant
(
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id    BIGINT NOT NULL COMMENT '用户ID',
    PRIMARY KEY (project_id, user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '项目参与人员';

-- 支持管理类项目设置多个项目经理
-- manager_id 保留第一位（主要负责人），co_manager_ids 存储其余负责人 ID（英文逗号分隔）
ALTER TABLE pf_project
    ADD COLUMN co_manager_ids VARCHAR(500) NULL COMMENT '协同项目经理ID列表，英文逗号分隔' AFTER manager_id;

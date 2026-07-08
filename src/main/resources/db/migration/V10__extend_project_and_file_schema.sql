-- ============================================================
-- V10: 扩展项目字段、调整项目类型枚举、
--      日报关联任务、文档文件夹
-- ============================================================

-- 1. pf_project 新增业务字段
ALTER TABLE pf_project
    ADD COLUMN business_department VARCHAR(128) NULL COMMENT '业务部门' AFTER contract_status,
    ADD COLUMN contractor_unit     VARCHAR(128) NULL COMMENT '承建单位'  AFTER business_department,
    ADD COLUMN business_supervisor VARCHAR(64)  NULL COMMENT '业务主管'  AFTER contractor_unit,
    ADD COLUMN receivable_amount   DECIMAL(15,2) NULL COMMENT '回款金额（元）' AFTER business_supervisor;

-- 2. 日报关联任务中间表
CREATE TABLE pf_daily_report_task (
    report_id BIGINT NOT NULL COMMENT '日报ID',
    task_id   BIGINT NOT NULL COMMENT '任务ID',
    PRIMARY KEY (report_id, task_id),
    INDEX idx_report (report_id),
    INDEX idx_task   (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日报关联任务';

-- 3. 文档文件夹
CREATE TABLE pf_file_folder (
    id            BIGINT       NOT NULL PRIMARY KEY COMMENT '主键（雪花ID）',
    business_type VARCHAR(64)  NOT NULL COMMENT '业务类型，例如 PROJECT',
    business_id   BIGINT       NOT NULL COMMENT '业务ID',
    name          VARCHAR(128) NOT NULL COMMENT '文件夹名称',
    created_by    BIGINT       NULL COMMENT '创建人',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_folder_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档文件夹';

-- 4. pf_file 增加所属文件夹字段
ALTER TABLE pf_file
    ADD COLUMN folder_id BIGINT NULL COMMENT '所属文件夹ID，NULL 表示根目录' AFTER file_category;

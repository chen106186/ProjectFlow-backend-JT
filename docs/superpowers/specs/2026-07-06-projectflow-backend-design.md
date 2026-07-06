# ProjectFlow Backend Design

## 1. 背景与目标

ProjectFlow 是面向内部项目开发与管理的平台，覆盖项目全生命周期中的项目清单、任务、需求、BUG、个人工作、系统设置、通知、附件和操作日志。第一版后端目标是先支撑 P0 主链路稳定落地：项目、任务、BUG、附件、权限、日志和首页待办统计。

后端采用 Java 21 与 Spring Boot 3，数据库使用 MySQL 8。系统先按模块化单体建设，减少早期部署、联调和跨服务事务复杂度，同时通过清晰的包结构和接口边界为后续拆分服务保留空间。

## 2. 技术选型

| 类型 | 选型 | 说明 |
| --- | --- | --- |
| 语言版本 | Java 21 | 使用 LTS 版本，适合长期维护 |
| 后端框架 | Spring Boot 3.5.x | 生态成熟，适合企业内部管理系统 |
| 构建工具 | Maven | 简洁稳定，团队接受度高 |
| 数据库 | MySQL 8 | 符合当前团队技术选择 |
| ORM | MyBatis-Plus | 兼顾开发效率和 SQL 可控性 |
| 数据库迁移 | Flyway | 管理表结构版本，避免手工同步 |
| 权限认证 | Spring Security + JWT | 支持无状态接口鉴权和角色权限控制 |
| 缓存 | Redis | 用于登录会话辅助、验证码、热点字典和待办统计缓存 |
| 接口文档 | springdoc-openapi / Swagger UI | 前后端联调可视化 |
| 文件存储 | 本地存储起步，预留阿里云 OSS | 第一版低成本落地，生产可平滑切换 OSS |
| Excel | EasyExcel | 支持任务、项目、日志等列表导出 |
| 测试 | JUnit 5 + Spring Boot Test | 覆盖核心状态流转和接口行为 |

## 3. 架构方案

采用模块化单体。所有业务模块部署在同一个 Spring Boot 应用内，模块之间通过 Service 接口协作。模块内部按 Controller、Service、Mapper、Entity、DTO、VO 分层，避免跨模块直接访问对方 Mapper 或数据库表。

推荐包结构：

```text
com.jitong.projectflow
├─ ProjectFlowApplication.java
├─ auth
├─ system
├─ project
├─ task
├─ requirement
├─ bug
├─ file
├─ notice
├─ dashboard
└─ common
```

模块职责：

| 模块 | 职责 |
| --- | --- |
| auth | 登录、JWT 生成与校验、当前用户上下文 |
| system | 用户、部门、岗位、角色、菜单权限、操作日志 |
| project | 管理类项目、执行类项目、项目阶段、项目节点、甘特图数据 |
| task | 全部任务、我的任务、任务详情、任务日历、任务状态流转 |
| requirement | 需求提交、需求编辑、需求详情、需求状态管理 |
| bug | BUG 提交、BUG 列表、我的 BUG、评论、指派、关闭 |
| file | 附件上传下载、文件元数据、文件版本、本地存储和 OSS 适配 |
| notice | 通知、已读未读、逾期预警、任务通知、BUG 通知 |
| dashboard | 首页统计卡片、待办清单、日历摘要 |
| common | 通用响应、异常、分页、枚举、审计字段、工具类 |

## 4. 数据与状态设计

所有核心业务表统一包含审计字段：

```text
id
created_by
created_at
updated_by
updated_at
deleted
```

建议第一版核心表：

| 表名 | 用途 |
| --- | --- |
| sys_user | 用户账号 |
| sys_department | 部门树 |
| sys_role | 角色 |
| sys_menu | 菜单与按钮权限 |
| sys_user_role | 用户角色关联 |
| sys_role_menu | 角色权限关联 |
| sys_operation_log | 操作日志 |
| pf_project | 项目主表，区分管理类和执行类 |
| pf_project_stage | 项目阶段 |
| pf_project_node | 项目节点，支撑甘特图 |
| pf_task | 任务主表 |
| pf_requirement | 需求主表 |
| pf_bug | BUG 主表 |
| pf_bug_comment | BUG 评论 |
| pf_file | 文件元数据 |
| pf_notice | 通知 |

任务状态建议：

```text
NOT_STARTED
IN_PROGRESS
DUE_SOON
OVERDUE
COMPLETED
PAUSED
```

任务状态根据计划时间和实际时间自动计算。实际开始时间填写后进入进行中；实际结束时间填写后进入已完成；未完成且当前日期超过计划结束时间进入已逾期；计划结束前三天内进入即将到期。

BUG 状态建议：

```text
PENDING_FIX
FIXING
PENDING_VERIFY
CLOSED
```

需求状态建议：

```text
PENDING_REVIEW
ACCEPTED
REJECTED
```

操作日志由后端自动生成，不允许前端直接写入日志表。日志记录用户、模块、业务 ID、操作类型、变更前后关键值和展示文本。

## 5. 文件存储设计

业务模块不直接操作本地文件路径或 OSS SDK，统一依赖文件存储接口：

```java
public interface FileStorageService {
    StoredFile upload(FileUploadCommand command);
    InputStream download(String storageKey);
    void delete(String storageKey);
}
```

第一版实现：

```text
LocalFileStorageService
```

生产预留实现：

```text
AliyunOssFileStorageService
```

可选兼容实现：

```text
MinioFileStorageService
```

文件元数据保存在 `pf_file`，字段建议包括业务类型、业务 ID、原始文件名、文件大小、文件类型、版本号、存储类型、存储 key、上传人、上传时间和删除状态。单文件限制 50MB。允许的文件类型包括 docx、xlsx、pdf、png、jpg、drawio，以及后续业务明确允许的开发材料格式。

## 6. 权限设计

第一版权限采用 RBAC：

```text
用户 -> 角色 -> 菜单/按钮权限
```

角色初始建议：

| 角色 | 权限范围 |
| --- | --- |
| 超级管理员 | 全部权限 |
| 总经办 | 项目、需求、BUG、统计、系统日志 |
| 项目经理 | 项目、任务、需求、BUG |
| 开发负责人 | 任务、需求、BUG |
| 设计/开发/测试 | 个人工作、任务、BUG |
| 综合部 | 用户、部门、角色管理 |

数据权限第一版按业务规则实现：我的任务按执行人和项目经理过滤；我的 BUG 按创建人和指定人过滤；系统操作日志所有人可查看但不可删除；用户与权限管理仅综合部、总经办和超级管理员可编辑。

## 7. API 分组

第一版接口按业务模块分组：

```text
/api/auth
/api/system/users
/api/system/departments
/api/system/roles
/api/system/menus
/api/system/logs
/api/projects
/api/tasks
/api/requirements
/api/bugs
/api/files
/api/notices
/api/dashboard
```

接口统一返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "request-trace-id"
}
```

分页接口统一接收 `pageNo`、`pageSize`，返回总数、页码、页大小和记录列表。

## 8. 错误处理与审计

后端统一异常处理，区分参数错误、认证失败、权限不足、资源不存在、业务状态冲突和系统异常。所有异常响应带 `traceId`，便于排查日志。

涉及状态变化的操作必须写操作日志，包括任务实际开始/结束时间、BUG 状态变更、BUG 指派、BUG 关闭、需求状态变更、项目节点时间变更、文件上传下载和用户启用禁用。

## 9. 测试策略

第一版重点测试核心业务规则：

| 范围 | 测试重点 |
| --- | --- |
| auth | 登录成功、登录失败、JWT 校验 |
| task | 状态自动计算、我的任务过滤、逾期与即将到期排序 |
| bug | 创建、指派、状态流转、关闭权限 |
| file | 上传限制、元数据写入、下载 |
| audit | 状态变更自动生成日志 |
| dashboard | 统计数量、待办排序规则 |

测试优先级先覆盖 Service 层业务规则，再覆盖关键 Controller 接口。

## 10. 第一版交付范围

第一阶段交付范围：

1. 初始化 Spring Boot 3 + Java 21 后端工程。
2. 接入 MySQL 8、Flyway、MyBatis-Plus。
3. 建立统一响应、统一异常、分页、审计字段。
4. 完成用户、角色、权限、登录 JWT。
5. 完成项目、任务、BUG 的 P0 主链路。
6. 完成本地附件上传下载，保留阿里云 OSS 适配接口。
7. 完成操作日志自动记录。
8. 完成首页统计卡片、待办清单和任务日历摘要接口。

需求管理、完整甘特图、通知中心、Excel 导出和我的统计进入第二阶段，但第一阶段的数据模型需要为这些能力预留字段和关联关系。

## 11. 设计结论

本项目第一版采用 Java 21、Spring Boot 3、MySQL 8、MyBatis-Plus、Redis 和模块化单体架构。文件存储从本地实现开始，通过 `FileStorageService` 抽象预留阿里云 OSS。该方案能以较低复杂度快速支撑 P0 业务闭环，同时保留后续扩展、对象存储切换和服务拆分空间。

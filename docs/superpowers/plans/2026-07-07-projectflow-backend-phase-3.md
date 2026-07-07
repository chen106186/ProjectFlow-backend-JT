# ProjectFlow Backend Phase 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace remaining P0 skeleton APIs with usable project, task, BUG, file, and system query workflows.

**Architecture:** Continue the existing modular monolith on the current branch. Each business module owns its entity, mapper, DTOs, service, and controller endpoints.

**Tech Stack:** Java 21, Spring Boot 3.5.x, MySQL 8, MyBatis-Plus, Flyway, JUnit 5, Mockito.

---

## Task 1: Phase 3 Business Index Migration

**Files:**
- Create: `src/main/resources/db/migration/V3__phase_3_business_indexes.sql`

- [x] **Step 1: Add migration**

Create `V3__phase_3_business_indexes.sql`:

```sql
CREATE INDEX idx_pf_project_name ON pf_project (name);
CREATE INDEX idx_pf_project_contract_status ON pf_project (contract_status);

CREATE INDEX idx_pf_task_status_priority ON pf_task (status, priority);
CREATE INDEX idx_pf_task_created_time ON pf_task (created_at);

CREATE INDEX idx_pf_bug_assignee_status ON pf_bug (assignee_id, status);
CREATE INDEX idx_pf_bug_created_time ON pf_bug (created_at);

CREATE INDEX idx_sys_user_department_enabled ON sys_user (department_id, enabled);
```

- [x] **Step 2: Verify migration file exists**

Run:

```bash
rg --files src/main/resources/db/migration
```

Expected: V1, V2, and V3 migration files exist.

- [x] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V3__phase_3_business_indexes.sql
git commit -m "feat: add phase 3 business indexes"
```

## Task 2: Project CRUD

**Files:**
- Create: `src/main/java/com/jitong/projectflow/project/entity/ProjectEntity.java`
- Create: `src/main/java/com/jitong/projectflow/project/mapper/ProjectMapper.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/ProjectCreateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/ProjectUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/ProjectQueryRequest.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/ProjectResponse.java`
- Create: `src/main/java/com/jitong/projectflow/project/service/ProjectService.java`
- Modify: `src/main/java/com/jitong/projectflow/project/controller/ProjectController.java`
- Test: `src/test/java/com/jitong/projectflow/project/service/ProjectServiceTest.java`

- [x] **Step 1: Write service tests**

Test project create maps request fields, default status, and writes an operation log.

- [x] **Step 2: Implement entity, mapper, DTOs, and service**

Rules:

- Create defaults missing status to `NOT_STARTED`.
- Detail not found throws `BusinessException(ErrorCode.NOT_FOUND, "Project not found")`.
- Delete uses MyBatis-Plus logical delete.
- Create, update, and delete call `OperationLogService.record(...)`.

- [x] **Step 3: Implement APIs**

```text
POST   /api/projects
GET    /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=ProjectServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/project src/test/java/com/jitong/projectflow/project
git commit -m "feat: implement project crud"
```

## Task 3: Task Workflow

**Files:**
- Create: `src/main/java/com/jitong/projectflow/task/dto/TaskCreateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/task/dto/TaskUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/task/dto/TaskActualTimeUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/task/dto/TaskQueryRequest.java`
- Create: `src/main/java/com/jitong/projectflow/task/dto/TaskResponse.java`
- Modify: `src/main/java/com/jitong/projectflow/task/mapper/TaskMapper.java`
- Create: `src/main/java/com/jitong/projectflow/task/service/TaskService.java`
- Modify: `src/main/java/com/jitong/projectflow/task/controller/TaskController.java`
- Test: `src/test/java/com/jitong/projectflow/task/service/TaskServiceTest.java`

- [x] **Step 1: Write service tests**

Test actual time update recalculates status using `TaskStatusCalculator`.

- [x] **Step 2: Implement service**

Rules:

- Create requires project id, name, assignee id, and priority.
- `GET /my` filters by `CurrentUserContext.userId()`.
- Actual time update writes operation log.
- Delete is logical delete.

- [x] **Step 3: Implement APIs**

```text
POST   /api/tasks
GET    /api/tasks
GET    /api/tasks/my
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
PATCH  /api/tasks/{id}/actual-time
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=TaskServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/task src/test/java/com/jitong/projectflow/task
git commit -m "feat: implement task workflow"
```

## Task 4: BUG Workflow and Comments

**Files:**
- Create: `src/main/java/com/jitong/projectflow/bug/entity/BugCommentEntity.java`
- Create: `src/main/java/com/jitong/projectflow/bug/mapper/BugCommentMapper.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugCreateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugAssignRequest.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugCommentCreateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugResponse.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugCommentResponse.java`
- Create: `src/main/java/com/jitong/projectflow/bug/service/BugService.java`
- Modify: `src/main/java/com/jitong/projectflow/bug/controller/BugController.java`
- Test: `src/test/java/com/jitong/projectflow/bug/service/BugServiceTest.java`

- [x] **Step 1: Write service tests**

Test assign changes assignee, close sets status to `CLOSED`, and comments are persisted.

- [x] **Step 2: Implement service**

Rules:

- My BUGs include creator or assignee.
- Assign writes operation log and creates a notice for the new assignee.
- Close sets `closedAt` and writes operation log.
- Comment writes comment row, operation log, and comment notice.

- [x] **Step 3: Implement APIs**

```text
POST   /api/bugs
GET    /api/bugs
GET    /api/bugs/my
GET    /api/bugs/{id}
PUT    /api/bugs/{id}
PATCH  /api/bugs/{id}/assign
PATCH  /api/bugs/{id}/close
POST   /api/bugs/{id}/comments
GET    /api/bugs/{id}/comments
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=BugServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/bug src/test/java/com/jitong/projectflow/bug
git commit -m "feat: implement bug workflow"
```

## Task 5: File Upload and Download APIs

**Files:**
- Create: `src/main/java/com/jitong/projectflow/file/dto/FileResponse.java`
- Create: `src/main/java/com/jitong/projectflow/file/service/FileService.java`
- Create: `src/main/java/com/jitong/projectflow/file/controller/FileController.java`
- Test: `src/test/java/com/jitong/projectflow/file/service/FileServiceTest.java`

- [ ] **Step 1: Write service tests**

Test upload delegates to `FileStorageService` and inserts `FileMetadata`.

- [ ] **Step 2: Implement service**

Rules:

- Upload stores physical file first, then metadata.
- List filters by business type and business id.
- Download returns metadata and stream.
- Delete logically deletes metadata and calls storage delete.

- [ ] **Step 3: Implement APIs**

```text
POST   /api/files
GET    /api/files
GET    /api/files/{id}/download
DELETE /api/files/{id}
```

- [ ] **Step 4: Verify**

```bash
mvn -q -Dtest=FileServiceTest test
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/file src/test/java/com/jitong/projectflow/file
git commit -m "feat: add file upload download api"
```

## Task 6: System Query APIs

**Files:**
- Create: `src/main/java/com/jitong/projectflow/system/entity/DepartmentEntity.java`
- Create: `src/main/java/com/jitong/projectflow/system/entity/RoleEntity.java`
- Create: `src/main/java/com/jitong/projectflow/system/entity/MenuEntity.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/DepartmentMapper.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/RoleMapper.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/MenuMapper.java`
- Create: `src/main/java/com/jitong/projectflow/system/dto/SystemUserResponse.java`
- Create: `src/main/java/com/jitong/projectflow/system/dto/DepartmentResponse.java`
- Create: `src/main/java/com/jitong/projectflow/system/dto/RoleResponse.java`
- Create: `src/main/java/com/jitong/projectflow/system/dto/MenuResponse.java`
- Create: `src/main/java/com/jitong/projectflow/system/service/SystemQueryService.java`
- Create: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/SystemQueryServiceTest.java`

- [ ] **Step 1: Write service tests**

Test users, departments, roles, and menus are mapped to response DTOs.

- [ ] **Step 2: Implement query service**

Rules:

- User list can filter by keyword, department id, and enabled.
- Department list returns flat rows for frontend tree assembly.
- Menu list returns flat rows for frontend tree assembly.

- [ ] **Step 3: Implement APIs**

```text
GET /api/system/users
GET /api/system/departments
GET /api/system/roles
GET /api/system/menus
```

- [ ] **Step 4: Verify**

```bash
mvn -q -Dtest=SystemQueryServiceTest test
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system
git commit -m "feat: add system query apis"
```

## Task 7: Phase 3 Verification

**Files:**
- Modify only files needed to fix concrete compile or test failures.

- [ ] **Step 1: Run full tests**

```bash
mvn test
```

- [ ] **Step 2: Build package**

```bash
mvn package
```

- [ ] **Step 3: Run application and check OpenAPI**

Start with a free local port and check `/v3/api-docs`.

- [ ] **Step 4: Commit fixes or verification state**

If fixes were needed:

```bash
git add pom.xml src/main/java src/main/resources src/test/java
git commit -m "fix: stabilize phase 3 business workflows"
```

If only the plan checklist changed:

```bash
git add docs/superpowers/plans/2026-07-07-projectflow-backend-phase-3.md
git commit -m "docs: mark phase 3 verification complete"
```

## Definition of Done

- Project CRUD APIs return real data.
- Task CRUD and my task APIs return real data.
- BUG workflow and comments are implemented.
- File upload/download APIs are implemented.
- System selector/query APIs are implemented.
- Full test suite and package build pass.
- Application starts and OpenAPI includes Phase 3 endpoints.

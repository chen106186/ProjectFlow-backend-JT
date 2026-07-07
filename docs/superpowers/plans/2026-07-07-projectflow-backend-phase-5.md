# ProjectFlow Backend Phase 5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden list APIs, validation, seed data, and RBAC join persistence for frontend integration.

**Architecture:** Reuse existing modular monolith services. Use MyBatis-Plus `Page<T>` with the existing `PageResult<T>`. Keep seed data in Flyway and avoid destructive schema changes.

**Tech Stack:** Java 21, Spring Boot 3.5.x, MyBatis-Plus, MySQL 8, Flyway, Jakarta Validation, JUnit 5, Mockito.

---

## Task 1: Shared Pagination Foundation

**Files:**
- Create: `src/main/java/com/jitong/projectflow/common/api/PageQuery.java`
- Create: `src/main/java/com/jitong/projectflow/common/api/PageUtils.java`
- Test: `src/test/java/com/jitong/projectflow/common/api/PageUtilsTest.java`

- [x] **Step 1: Write tests**

Test default page values and max page size.

- [x] **Step 2: Implement pagination helpers**

Rules:

- Default `pageNo = 1`.
- Default `pageSize = 20`.
- `pageSize` caps at 200.

- [x] **Step 3: Verify**

```bash
mvn -q -Dtest=PageUtilsTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/common src/test/java/com/jitong/projectflow/common docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "feat: add pagination foundation"
```

## Task 2: Business List Pagination

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/project/dto/ProjectQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/project/service/ProjectService.java`
- Modify: `src/main/java/com/jitong/projectflow/project/controller/ProjectController.java`
- Modify: `src/main/java/com/jitong/projectflow/task/dto/TaskQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/task/service/TaskService.java`
- Modify: `src/main/java/com/jitong/projectflow/task/controller/TaskController.java`
- Create: `src/main/java/com/jitong/projectflow/bug/dto/BugQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/bug/service/BugService.java`
- Modify: `src/main/java/com/jitong/projectflow/bug/controller/BugController.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/dto/RequirementQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/requirement/service/RequirementService.java`
- Modify: `src/main/java/com/jitong/projectflow/requirement/controller/RequirementController.java`
- Tests: existing service tests plus targeted pagination assertions.

- [x] **Step 1: Write tests**

Add/adjust tests for project, task, BUG, and requirement list methods returning `PageResult`.

- [x] **Step 2: Implement paginated business services and controllers**

Rules:

- `GET /api/projects`, `/api/tasks`, `/api/bugs`, `/api/requirements` return `PageResult`.
- `/my` endpoints stay list responses in Phase 5.

- [x] **Step 3: Verify**

```bash
mvn -q -Dtest=ProjectServiceTest,TaskServiceTest,BugServiceTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/project src/main/java/com/jitong/projectflow/task src/main/java/com/jitong/projectflow/bug src/main/java/com/jitong/projectflow/requirement src/test/java/com/jitong/projectflow docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "feat: paginate business list apis"
```

## Task 3: System List Pagination

**Files:**
- Create: `src/main/java/com/jitong/projectflow/system/dto/SystemUserQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/system/dto/OperationLogQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/system/service/SystemQueryService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/service/OperationLogQueryService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Tests: `SystemQueryServiceTest`, `OperationLogQueryServiceTest`

- [x] **Step 1: Write tests**

Test user and operation log pagination maps total and records.

- [x] **Step 2: Implement system pagination**

Rules:

- `GET /api/system/users` returns `PageResult`.
- `GET /api/system/logs` returns `PageResult`.
- Department, role, and menu selector APIs stay lists.

- [x] **Step 3: Verify**

```bash
mvn -q -Dtest=SystemQueryServiceTest,OperationLogQueryServiceTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "feat: paginate system list apis"
```

## Task 4: Validation Hardening

**Files:**
- Modify create/request DTOs under project, task, bug, requirement, and system modules.
- Modify controllers only where `@Valid` is missing.
- Test: `src/test/java/com/jitong/projectflow/common/error/GlobalExceptionHandlerTest.java` or focused controller test.

- [ ] **Step 1: Add validation annotations**

Rules:

- Required ids use `@NotNull`.
- Required names/titles use `@NotBlank`.
- Page bounds use `@Min` and `@Max`.

- [ ] **Step 2: Verify**

```bash
mvn -q test
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java src/test/java docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "feat: harden request validation"
```

## Task 5: Seed Data Migration

**Files:**
- Create: `src/main/resources/db/migration/V5__phase_5_seed_admin_data.sql`

- [ ] **Step 1: Add seed migration**

Seed baseline department, admin role, admin user, menus, user-role, and role-menu rows. Use idempotent `INSERT ... SELECT ... WHERE NOT EXISTS`.

- [ ] **Step 2: Verify migration file exists**

```bash
rg --files src/main/resources/db/migration
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V5__phase_5_seed_admin_data.sql docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "feat: seed admin integration data"
```

## Task 6: RBAC Join Mapper Cleanup

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/system/mapper/UserRoleMapper.java`
- Modify: `src/main/java/com/jitong/projectflow/system/mapper/RoleMenuMapper.java`
- Modify: system services using these mappers.
- Tests: `SystemUserManagementServiceTest`, `RoleManagementServiceTest`, `CurrentUserPermissionServiceTest`, `SecurityConfigTest`

- [ ] **Step 1: Replace BaseMapper join usage with explicit SQL methods**

Rules:

- Avoid MyBatis-Plus no-primary-key warnings for join entities.
- Preserve existing user-role and role-menu behavior.

- [ ] **Step 2: Verify**

```bash
mvn -q -Dtest=SystemUserManagementServiceTest,RoleManagementServiceTest,CurrentUserPermissionServiceTest,SecurityConfigTest test
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/auth docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "fix: use explicit rbac join mappers"
```

## Task 7: Phase 5 Verification

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

- [ ] **Step 4: Commit verification state**

```bash
git add docs/superpowers/plans/2026-07-07-projectflow-backend-phase-5.md
git commit -m "docs: mark phase 5 verification complete"
```

## Definition of Done

- Table-style list APIs return `PageResult`.
- Page defaults and max page size are enforced.
- Request DTOs have clear validation for required fields.
- Fresh database has admin seed data for frontend login.
- RBAC join mapper warnings are removed.
- Full tests and package build pass.
- Application starts and OpenAPI exposes Phase 5 contracts.

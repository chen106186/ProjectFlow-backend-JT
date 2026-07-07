# ProjectFlow Backend Phase 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the first usable system administration loop: user, department, role, menu permission, current-user permissions, operation logs, and JWT protection.

**Architecture:** Continue the modular monolith. Keep all administration code inside `com.jitong.projectflow.system`, use MyBatis-Plus mappers for existing RBAC tables, and keep authentication code inside `com.jitong.projectflow.auth.security`.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Security, MyBatis-Plus, MySQL 8, Flyway, JUnit 5, Mockito.

---

## Task 1: Phase 4 System Index Migration

**Files:**
- Create: `src/main/resources/db/migration/V4__phase_4_system_management_indexes.sql`

- [x] **Step 1: Add migration**

Create indexes for role/menu joins, tree sorting, and operation log filters:

```sql
CREATE INDEX idx_sys_user_role_role ON sys_user_role (role_id);
CREATE INDEX idx_sys_role_menu_menu ON sys_role_menu (menu_id);
CREATE INDEX idx_sys_menu_parent_sort ON sys_menu (parent_id, sort_order);
CREATE INDEX idx_sys_department_parent_sort ON sys_department (parent_id, sort_order);
CREATE INDEX idx_sys_operation_log_module_type_time ON sys_operation_log (module, operation_type, created_at);
```

- [x] **Step 2: Verify migration file exists**

Run:

```bash
rg --files src/main/resources/db/migration
```

Expected: V1, V2, V3, and V4 migration files exist.

- [x] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V4__phase_4_system_management_indexes.sql docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: add phase 4 system indexes"
```

## Task 2: User Management

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/system/entity/SystemUser.java`
- Create: `src/main/java/com/jitong/projectflow/system/entity/UserRoleEntity.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/UserRoleMapper.java`
- Create DTOs under `src/main/java/com/jitong/projectflow/system/dto`
- Create: `src/main/java/com/jitong/projectflow/system/service/SystemUserManagementService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/SystemUserManagementServiceTest.java`

- [x] **Step 1: Write service tests**

Cover create password hashing, enable-disable, and role replacement.

- [x] **Step 2: Implement user management service**

Rules:

- Create requires unique username.
- Password uses `PasswordEncoder`.
- Update does not overwrite password.
- Enable-disable writes operation log.
- Role assignment deletes existing user-role rows and inserts the new set.

- [x] **Step 3: Add APIs**

```text
POST   /api/system/users
GET    /api/system/users/{id}
PUT    /api/system/users/{id}
PATCH  /api/system/users/{id}/enabled
PATCH  /api/system/users/{id}/password
PUT    /api/system/users/{id}/roles
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=SystemUserManagementServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: add system user management"
```

## Task 3: Department Management

**Files:**
- Create DTOs under `src/main/java/com/jitong/projectflow/system/dto`
- Create: `src/main/java/com/jitong/projectflow/system/service/DepartmentManagementService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/DepartmentManagementServiceTest.java`

- [x] **Step 1: Write service tests**

Cover delete blocked by active users and child departments.

- [x] **Step 2: Implement department management service**

Rules:

- Create and update set parent id, name, and sort order.
- Delete is logical delete.
- Delete is blocked if active users or child departments exist.
- Write operation logs for create, update, and delete.

- [x] **Step 3: Add APIs**

```text
POST   /api/system/departments
PUT    /api/system/departments/{id}
DELETE /api/system/departments/{id}
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=DepartmentManagementServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: add department management"
```

## Task 4: Role and Menu Permission Management

**Files:**
- Create: `src/main/java/com/jitong/projectflow/system/entity/RoleMenuEntity.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/RoleMenuMapper.java`
- Create DTOs under `src/main/java/com/jitong/projectflow/system/dto`
- Create: `src/main/java/com/jitong/projectflow/system/service/RoleManagementService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/RoleManagementServiceTest.java`

- [x] **Step 1: Write service tests**

Cover role create uniqueness, role-menu replacement, and delete blocked by active users.

- [x] **Step 2: Implement role management service**

Rules:

- Role code is unique.
- Delete is logical delete.
- Delete is blocked if active users are assigned to the role.
- Menu assignment replaces existing role-menu rows.
- Write operation logs.

- [x] **Step 3: Add APIs**

```text
POST   /api/system/roles
GET    /api/system/roles/{id}
PUT    /api/system/roles/{id}
DELETE /api/system/roles/{id}
GET    /api/system/roles/{id}/menus
PUT    /api/system/roles/{id}/menus
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=RoleManagementServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: add role permission management"
```

## Task 5: Menu Management

**Files:**
- Create DTOs under `src/main/java/com/jitong/projectflow/system/dto`
- Create: `src/main/java/com/jitong/projectflow/system/service/MenuManagementService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/MenuManagementServiceTest.java`

- [x] **Step 1: Write service tests**

Cover invalid type, duplicate code, and delete blocked by child menu rows.

- [x] **Step 2: Implement menu management service**

Rules:

- Menu type must be `MENU` or `BUTTON`.
- Menu code is unique.
- Delete is logical delete.
- Delete is blocked if children exist.
- Write operation logs.

- [x] **Step 3: Add APIs**

```text
POST   /api/system/menus
PUT    /api/system/menus/{id}
DELETE /api/system/menus/{id}
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=MenuManagementServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: add menu management"
```

## Task 6: Current User Permissions and Operation Logs

**Files:**
- Create DTOs under `src/main/java/com/jitong/projectflow/system/dto`
- Create: `src/main/java/com/jitong/projectflow/system/service/CurrentUserPermissionService.java`
- Create: `src/main/java/com/jitong/projectflow/system/service/OperationLogQueryService.java`
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/CurrentUserPermissionServiceTest.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/OperationLogQueryServiceTest.java`

- [x] **Step 1: Write service tests**

Cover permission-code deduplication and operation-log filter mapping.

- [x] **Step 2: Implement services**

Rules:

- `/me` returns current user profile, roles, menus, and permissions.
- `/me/permissions` returns only permission codes.
- `/logs` filters by module, business type, business id, operation type, operator id, and time range.

- [x] **Step 3: Add APIs**

```text
GET /api/system/me
GET /api/system/me/permissions
GET /api/system/logs
```

- [x] **Step 4: Verify**

```bash
mvn -q -Dtest=CurrentUserPermissionServiceTest,OperationLogQueryServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: add user permissions and log queries"
```

## Task 7: JWT Protection

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/auth/security/JwtAuthenticationFilter.java`
- Modify: `src/main/java/com/jitong/projectflow/auth/security/SecurityConfig.java`
- Test: `src/test/java/com/jitong/projectflow/auth/security/SecurityConfigTest.java`

- [x] **Step 1: Write security test**

Cover non-public endpoint without JWT returns 401.

- [x] **Step 2: Implement authentication token and security rules**

Rules:

- Permit `/api/auth/login`, `/swagger-ui/**`, and `/v3/api-docs/**`.
- Require authentication for all other endpoints.
- JWT filter sets both `CurrentUserContext` and Spring Security authentication.
- Missing or invalid JWT returns 401 for protected endpoints.

- [x] **Step 3: Verify**

```bash
mvn -q -Dtest=SecurityConfigTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/auth src/test/java/com/jitong/projectflow/auth docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "feat: protect api endpoints with jwt"
```

## Task 8: Phase 4 Verification

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
git add docs/superpowers/plans/2026-07-07-projectflow-backend-phase-4.md
git commit -m "docs: mark phase 4 verification complete"
```

## Definition of Done

- System users can be created, edited, disabled, password-reset, and assigned roles.
- Departments, roles, and menus can be managed with safe logical deletes.
- Role-menu and user-role assignments are persisted.
- Current user profile and permission codes can be queried.
- Operation logs can be queried for administration pages.
- Non-public APIs require JWT.
- Full test suite and package build pass.
- Application starts and OpenAPI includes Phase 4 endpoints.

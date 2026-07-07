# ProjectFlow Backend Phase 6 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce RBAC permission codes on system administration and core business write APIs.

**Architecture:** Keep the modular monolith and existing `sys_menu` permission vocabulary. Load permission codes into Spring Security authorities during JWT authentication, then protect controller methods with `@PreAuthorize`.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Security method authorization, MyBatis-Plus, MySQL 8, Flyway, JUnit 5, Mockito, MockMvc.

---

## Task 1: Permission Lookup for JWT Authentication

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/system/service/CurrentUserPermissionService.java`
- Modify: `src/main/java/com/jitong/projectflow/auth/security/JwtAuthenticationFilter.java`
- Modify: `src/main/java/com/jitong/projectflow/auth/security/SecurityConfig.java`
- Test: `src/test/java/com/jitong/projectflow/system/service/CurrentUserPermissionServiceTest.java`
- Test: `src/test/java/com/jitong/projectflow/auth/security/SecurityConfigTest.java`

- [x] **Step 1: Add user-id permission lookup test**

Add a test that calls `getPermissionsByUserId(1L)` and verifies duplicate menu ids and duplicate menu codes are deduplicated.

Expected setup:

```java
when(systemUserMapper.selectById(1L)).thenReturn(user);
when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(10L))).thenReturn(List.of(100L, 100L));
when(menuMapper.selectList(any())).thenReturn(List.of(menu, menu));
```

Expected assertion:

```java
assertThat(service.getPermissionsByUserId(1L)).containsExactly("system:user:view");
```

- [x] **Step 2: Implement user-id permission lookup**

Add `public List<String> getPermissionsByUserId(Long userId)` to `CurrentUserPermissionService`.

Rules:

- Throw `BusinessException(ErrorCode.UNAUTHORIZED, "Current user not found")` when the user does not exist.
- Throw `BusinessException(ErrorCode.FORBIDDEN, "Current user disabled")` when disabled.
- Return deduplicated permission codes from all assigned menu rows.
- Make `getCurrentUser()` reuse the same role/menu lookup style where practical.

- [x] **Step 3: Inject permissions into JWT authentication**

Change `JwtAuthenticationFilter` constructor to accept `CurrentUserPermissionService`.

When token parsing succeeds:

```java
List<SimpleGrantedAuthority> authorities = currentUserPermissionService.getPermissionsByUserId(userId).stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userId, null, authorities);
```

If lookup throws because the user is missing or disabled, log debug and do not install authentication.

- [x] **Step 4: Enable method security**

Update `SecurityConfig`:

- Add `@EnableMethodSecurity`.
- Construct `JwtAuthenticationFilter(jwtTokenService, currentUserPermissionService)`.

- [x] **Step 5: Verify**

Run:

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=CurrentUserPermissionServiceTest,SecurityConfigTest test
```

- [x] **Step 6: Commit**

```bash
git add src/main/java/com/jitong/projectflow/auth src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/auth src/test/java/com/jitong/projectflow/system docs/superpowers/plans/2026-07-07-projectflow-backend-phase-6.md
git commit -m "feat: load rbac permissions into jwt authentication"
```

## Task 2: JSON Authentication and Access Denied Responses

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/auth/security/SecurityConfig.java`
- Create: `src/main/java/com/jitong/projectflow/auth/security/SecurityErrorResponseWriter.java`
- Test: `src/test/java/com/jitong/projectflow/auth/security/SecurityConfigTest.java`

- [x] **Step 1: Add security response tests**

Extend `SecurityConfigTest` with:

- Anonymous request to `/api/system/users` returns HTTP 401 and JSON body code `401`.
- Authenticated request without required authority to a protected method returns HTTP 403 and JSON body code `403`.

Use the existing MockMvc pattern and JWT token helper already present in the test.

- [x] **Step 2: Add JSON response writer**

Create `SecurityErrorResponseWriter` with a method:

```java
void write(HttpServletResponse response, int status, String message) throws IOException
```

It should write:

```json
{"code":403,"message":"Forbidden","data":null,"traceId":"..."}
```

Use `TraceIdFilter.TRACE_ID` from MDC when present.

- [x] **Step 3: Wire 401 and 403 handlers**

In `SecurityConfig.exceptionHandling`:

- `authenticationEntryPoint` writes `401 Unauthorized`.
- `accessDeniedHandler` writes `403 Forbidden`.

- [x] **Step 4: Verify**

Run:

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=SecurityConfigTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/auth src/test/java/com/jitong/projectflow/auth docs/superpowers/plans/2026-07-07-projectflow-backend-phase-6.md
git commit -m "feat: return json security errors"
```

## Task 3: System Management Method Permissions

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/system/controller/SystemController.java`
- Test: `src/test/java/com/jitong/projectflow/auth/security/SecurityConfigTest.java`

- [x] **Step 1: Add method security tests**

Add MockMvc tests proving:

- A token with `system:user:view` can access `GET /api/system/users`.
- A token without `system:user:view` receives `403`.
- `/api/system/me` still only requires authentication.

- [x] **Step 2: Add system `@PreAuthorize` annotations**

Protect methods with these permission codes:

- Logs: `system:log:view`.
- User list/detail: `system:user:view`.
- User create: `system:user:create`.
- User update, enable/disable, reset password, assign roles: `system:user:update`.
- Department list: `system:department:view`.
- Department create: `system:department:create`.
- Department update/delete: `system:department:update`.
- Role list/detail/menu ids: `system:role:view`.
- Role create: `system:role:create`.
- Role update/delete: `system:role:update`.
- Role menu assignment: `system:role:assign-menu`.
- Menu list: `system:menu:view`.
- Menu create: `system:menu:create`.
- Menu update/delete: `system:menu:update`.

- [x] **Step 3: Verify**

Run:

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=SecurityConfigTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/auth docs/superpowers/plans/2026-07-07-projectflow-backend-phase-6.md
git commit -m "feat: protect system management apis"
```

## Task 4: Core Business Write Permissions

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/project/controller/ProjectController.java`
- Modify: `src/main/java/com/jitong/projectflow/task/controller/TaskController.java`
- Modify: `src/main/java/com/jitong/projectflow/requirement/controller/RequirementController.java`
- Modify: `src/main/java/com/jitong/projectflow/bug/controller/BugController.java`
- Test: `src/test/java/com/jitong/projectflow/auth/security/SecurityConfigTest.java`

- [x] **Step 1: Add business permission tests**

Add MockMvc tests proving:

- Authenticated users can still call a business read endpoint without action permission.
- Missing `task:update` blocks `PUT /api/tasks/{id}` with `403`.
- Having `task:update` allows the request to reach the controller layer.

- [x] **Step 2: Add business `@PreAuthorize` annotations**

Protect methods with these permission codes:

- Project create: `project:create`.
- Project update, delete, gantt node update: `project:update`.
- Task create: `task:create`.
- Task update, delete, actual-time update: `task:update`.
- Requirement create: `requirement:create`.
- Requirement update and status update: `requirement:update`.
- BUG create: `bug:create`.
- BUG update, assign, close, add comment: `bug:update`.

Leave list/detail/read endpoints authenticated-only.

- [x] **Step 3: Verify**

Run:

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=SecurityConfigTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/project src/main/java/com/jitong/projectflow/task src/main/java/com/jitong/projectflow/requirement src/main/java/com/jitong/projectflow/bug src/test/java/com/jitong/projectflow/auth docs/superpowers/plans/2026-07-07-projectflow-backend-phase-6.md
git commit -m "feat: protect business write apis"
```

## Task 5: Phase 6 Permission Seed Migration

**Files:**
- Create: `src/main/resources/db/migration/V6__phase_6_permission_seed.sql`

- [ ] **Step 1: Add migration**

Seed the following permission codes as `BUTTON` entries and assign them to admin role `1000000000000000101`:

```text
system:log:view
system:user:view
system:user:create
system:user:update
system:department:view
system:department:create
system:department:update
system:role:view
system:role:create
system:role:update
system:role:assign-menu
system:menu:view
system:menu:create
system:menu:update
project:create
project:update
task:create
task:update
requirement:create
requirement:update
bug:create
bug:update
```

Also seed business menu roots if absent:

```text
project
task
requirement
bug
```

- [ ] **Step 2: Verify migration file exists**

Run:

```bash
rg --files src/main/resources/db/migration
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V6__phase_6_permission_seed.sql docs/superpowers/plans/2026-07-07-projectflow-backend-phase-6.md
git commit -m "feat: seed phase 6 permissions"
```

## Task 6: Phase 6 Verification

**Files:**
- Modify only files needed to fix concrete compile or test failures.

- [ ] **Step 1: Run full tests**

```bash
mvn "-Dmaven.repo.local=.m2/repository" test
```

- [ ] **Step 2: Build package**

```bash
mvn "-Dmaven.repo.local=.m2/repository" package
```

- [ ] **Step 3: Run application and check OpenAPI**

Start with a free local port and check `/v3/api-docs`.

- [ ] **Step 4: Commit verification state**

```bash
git add docs/superpowers/plans/2026-07-07-projectflow-backend-phase-6.md
git commit -m "docs: mark phase 6 verification complete"
```

## Definition of Done

- JWT authentication contains RBAC permission codes as authorities.
- System management APIs are protected by explicit `system:*` permissions.
- Core business write APIs are protected by explicit action permissions.
- Login, OpenAPI, `/api/system/me`, and business read APIs remain usable with the intended access level.
- Security failures return JSON `ApiResponse` bodies for `401` and `403`.
- Admin seed data includes all Phase 6 permission codes.
- Full tests and package build pass.
- Application starts and OpenAPI exposes Phase 6 contracts.

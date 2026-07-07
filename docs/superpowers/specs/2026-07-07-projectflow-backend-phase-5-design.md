# ProjectFlow Backend Phase 5 Design

## 1. Goal

Phase 5 hardens the backend for frontend integration and daily test usage. Earlier phases made the main workflows usable. This phase focuses on predictable list responses, safer validation, startup seed data, and removal of avoidable runtime warnings.

The phase stays on the existing branch and keeps the modular monolith architecture.

## 2. Current State

Current backend state after Phase 4:

- Core workflow list APIs mostly return `List<T>`.
- `PageResult<T>` and MyBatis-Plus pagination are already available.
- JWT protection is enabled for non-public APIs.
- RBAC join entities work but emit MyBatis-Plus warnings because they have composite keys without `@TableId`.
- Local MySQL has schema migrations through V4.
- No seed data migration guarantees an admin user, baseline roles, or baseline menus for first-run frontend login.

## 3. Scope

### In Scope

- Add shared pagination request defaults.
- Convert high-traffic table APIs to `PageResult<T>`:
  - Projects
  - Tasks
  - BUGs
  - Requirements
  - System users
  - Operation logs
- Keep `my` focused APIs as lists unless they are table-style pages.
- Add validation annotations to request DTOs where missing for create/update safety.
- Add a V5 seed-data migration for baseline departments, admin role, admin user, baseline menus, and role assignments.
- Remove MyBatis-Plus join-table warnings by using explicit mapper SQL for `sys_user_role` and `sys_role_menu`.
- Add focused tests for pagination and seed-friendly behavior.

### Out of Scope

- Frontend changes.
- Cursor pagination.
- Large reporting pagination for Excel exports.
- Fine-grained backend permission annotations.
- Production password rotation policy.
- Complex seed-data customization by environment.

## 4. Pagination Design

Add a reusable request base:

```text
pageNo: default 1, min 1
pageSize: default 20, min 1, max 200
```

Each paginated service uses MyBatis-Plus `Page<T>` and maps the result to existing `PageResult<T>`.

Controller responses change from:

```text
ApiResponse<List<ProjectResponse>>
```

to:

```text
ApiResponse<PageResult<ProjectResponse>>
```

for table APIs.

Affected APIs:

```text
GET /api/projects
GET /api/tasks
GET /api/bugs
GET /api/requirements
GET /api/system/users
GET /api/system/logs
```

`GET /api/tasks/my`, `GET /api/bugs/my`, and notification inbox APIs stay as lists in Phase 5 to avoid broad frontend contract churn.

## 5. Validation Design

Use Jakarta Validation on request DTOs and controller `@Valid` where appropriate.

Examples:

- Project create requires `projectType`, `name`, and `status` defaults safely if omitted.
- Task create requires `projectId`, `name`, `assigneeId`, and `priority`.
- BUG create requires `projectId`, `title`, `assigneeId`, `description`, and `reproduceSteps`.
- System user create already requires username, password, and real name.
- Page request bounds prevent accidental huge queries.

Validation failures continue to use existing `GlobalExceptionHandler` and return `BAD_REQUEST`.

## 6. Seed Data Design

Add migration:

```text
V5__phase_5_seed_admin_data.sql
```

Seed data uses stable ids and `INSERT ... SELECT ... WHERE NOT EXISTS` to stay idempotent for fresh databases.

Baseline records:

- Department: `综合部`
- User: `admin`
- Role: `ADMIN`
- Menus:
  - `system`
  - `system:user`
  - `system:role`
  - `system:department`
  - `system:menu`
  - `system:log`
- Assign admin user to admin role.
- Assign all baseline menus to admin role.

The seed password is a BCrypt hash for `admin123`. This is only for development and first-run integration. Production must reset it.

## 7. Join Table Warning Cleanup

MyBatis-Plus warns when a `BaseMapper` entity has no single primary key. The RBAC join tables use composite primary keys by design.

Replace `BaseMapper<UserRoleEntity>` and `BaseMapper<RoleMenuEntity>` usage with explicit mapper methods:

```text
deleteByUserId
insertRelation
selectRoleIdsByUserId
selectByRoleId
deleteByRoleId
selectMenuIdsByRoleId
selectMenuIdsByRoleIds
```

Use annotation SQL for these focused methods. This removes the need for join entities to pretend they have a single id.

## 8. Testing Strategy

Priority tests:

- Project list returns `PageResult` total and records.
- Task list returns `PageResult`.
- BUG list returns `PageResult`.
- Requirement list returns `PageResult`.
- System user list returns `PageResult`.
- Operation log list returns `PageResult`.
- Role assignment and current-user permission tests still pass after mapper cleanup.
- Security test still confirms protected endpoints require JWT.

Full verification remains:

```text
mvn test
mvn package
start application and check /v3/api-docs
```

## 9. Delivery Order

1. Add pagination base request and pagination helpers.
2. Convert project/task/BUG/requirement list APIs to `PageResult`.
3. Convert system user and operation log list APIs to `PageResult`.
4. Add validation annotations.
5. Add V5 seed data.
6. Replace RBAC join mappers with explicit SQL methods.
7. Full verification.

## 10. Design Conclusion

Phase 5 makes the backend easier to wire from the frontend and cleaner to run locally. It improves API contracts without changing the core business model built in previous phases.

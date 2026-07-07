# ProjectFlow Backend Phase 4 Design

## 1. Goal

Phase 4 completes the first usable system administration loop. Phase 3 added read-only system selector APIs. Phase 4 turns that foundation into editable user, department, role, menu-permission, operation-log, and basic security workflows.

The phase stays on the existing branch and continues the modular monolith architecture. It does not introduce a separate identity service or a complex permission engine.

## 2. Current State

Current backend state after Phase 3:

- `sys_user`, `sys_department`, `sys_role`, `sys_menu`, `sys_user_role`, `sys_role_menu`, and `sys_operation_log` tables already exist.
- User, department, role, and menu read APIs already exist under `/api/system`.
- Login issues JWT tokens and `JwtAuthenticationFilter` can populate `CurrentUserContext`.
- `SecurityConfig` currently permits all requests except it still registers the JWT filter.
- Operation logs are written by business services, but no query API exists for system log pages.

## 3. Scope

### In Scope

- User create, update, detail, enable-disable, password reset, and role assignment.
- Department create, update, and soft delete.
- Role create, update, soft delete, detail, and menu assignment.
- Menu create, update, and soft delete for both menu and button permission rows.
- Current user profile API with roles, menus, and permission codes.
- Operation log list API with filters.
- Basic JWT protection for non-public APIs.
- Operation logs for system administration write operations.
- Service-level tests for user, role, permission, log, and security behavior.

### Out of Scope

- Frontend implementation.
- Data-scope permission rules such as department-only project visibility.
- Full workflow approval engine.
- OAuth, SSO, LDAP, or third-party identity integration.
- Fine-grained method annotation permissions for every business endpoint.
- Complex password policy, MFA, account lockout, or captcha.
- Physical deletion of users, roles, departments, or menus.

## 4. Design Approach

Use a practical RBAC model:

```text
User -> UserRole -> Role -> RoleMenu -> Menu
```

`sys_menu.code` is the permission code. Menu rows with `type = MENU` drive frontend navigation. Rows with `type = BUTTON` drive button-level frontend capability checks.

Phase 4 security has two layers:

1. JWT authentication protects all non-public endpoints.
2. Current-user permission APIs expose roles and permission codes for frontend rendering.

Backend method-level permission enforcement is intentionally deferred. This keeps Phase 4 focused on system management closure and avoids breaking existing P0 business APIs before the frontend has a permission model wired in.

## 5. Module Design

### system user management

Add editable user management APIs.

APIs:

```text
POST   /api/system/users
GET    /api/system/users/{id}
PUT    /api/system/users/{id}
PATCH  /api/system/users/{id}/enabled
PATCH  /api/system/users/{id}/password
PUT    /api/system/users/{id}/roles
```

Rules:

- Username is unique.
- New users require username, real name, password, and enabled flag.
- Passwords are stored using the existing `PasswordEncoder`.
- Disabled users cannot log in.
- User deletion is not included in Phase 4. Use disable for account retirement.
- Role assignment replaces the user's existing role rows.
- Create, update, enable-disable, reset password, and assign roles write operation logs.

### department management

Add basic editable department management.

APIs:

```text
POST   /api/system/departments
PUT    /api/system/departments/{id}
DELETE /api/system/departments/{id}
```

Rules:

- Department delete is logical delete.
- A department with active users cannot be deleted.
- A department with child departments cannot be deleted.
- Create, update, and delete write operation logs.

### role and permission management

Add role management and menu assignment.

APIs:

```text
POST   /api/system/roles
GET    /api/system/roles/{id}
PUT    /api/system/roles/{id}
DELETE /api/system/roles/{id}
GET    /api/system/roles/{id}/menus
PUT    /api/system/roles/{id}/menus
```

Rules:

- Role code is unique.
- Role delete is logical delete.
- A role assigned to active users cannot be deleted.
- Menu assignment replaces the role's existing menu rows.
- Create, update, delete, and menu assignment write operation logs.

### menu management

Add editable menu and permission-code management.

APIs:

```text
POST   /api/system/menus
PUT    /api/system/menus/{id}
DELETE /api/system/menus/{id}
```

Rules:

- Menu code is unique.
- `type` is `MENU` or `BUTTON`.
- Delete is logical delete.
- A menu with child menu rows cannot be deleted.
- Create, update, and delete write operation logs.

### current user permissions

Add APIs for frontend shell initialization.

APIs:

```text
GET /api/system/me
GET /api/system/me/permissions
```

Response includes:

- Current user id, username, real name, department id.
- Role ids and role codes.
- Menu rows for navigation.
- Permission codes from all assigned menu rows.

Rules:

- The API reads current user id from `CurrentUserContext`.
- Missing or disabled current user returns `UNAUTHORIZED` or `FORBIDDEN`.

### operation logs

Add an operation log query API.

APIs:

```text
GET /api/system/logs
```

Filters:

- `module`
- `businessType`
- `businessId`
- `operationType`
- `operatorId`
- `startTime`
- `endTime`

Rules:

- Results are ordered by `created_at desc`.
- Phase 4 returns a list, not paginated results, to stay consistent with current service patterns. Pagination belongs to a future reporting-hardening phase.

### security

Tighten HTTP security:

```text
Permit:
- /api/auth/login
- /swagger-ui/**
- /v3/api-docs/**

Require authenticated current user:
- all other endpoints
```

The existing `JwtAuthenticationFilter` should become responsible for setting a Spring Security authentication token, not only `CurrentUserContext`. This lets Spring Security reject unauthenticated requests consistently.

## 6. Data Changes

Add a Phase 4 migration:

```text
V4__phase_4_system_management_indexes.sql
```

Expected indexes:

- `sys_user_role(role_id)`
- `sys_role_menu(menu_id)`
- `sys_menu(parent_id, sort_order)`
- `sys_department(parent_id, sort_order)`
- `sys_operation_log(module, operation_type, created_at)`

No destructive schema changes are required.

## 7. Error Handling

Use existing `BusinessException` and `ErrorCode`.

Important cases:

- Duplicate username or role/menu code returns `CONFLICT`.
- User, role, department, menu, or log-related parent record not found returns `NOT_FOUND`.
- Deleting an entity still in use returns `CONFLICT`.
- Missing JWT returns `UNAUTHORIZED`.
- Disabled current user returns `FORBIDDEN`.

## 8. Testing Strategy

Priority tests:

- User create hashes password and writes operation log.
- Disabled user cannot log in.
- Assigning roles replaces old user-role rows.
- Department delete is blocked when active users or children exist.
- Role menu assignment replaces old role-menu rows.
- Role delete is blocked when assigned to active users.
- Current user permission API merges role menus and deduplicates codes.
- Operation log query maps filters into the expected response.
- Protected endpoint without JWT returns unauthorized.

Use service-level tests for business behavior and one focused security test for the authentication boundary.

## 9. Delivery Order

1. Phase 4 system indexes.
2. User management.
3. Department management.
4. Role and role-menu management.
5. Menu management.
6. Current user permission APIs.
7. Operation log query APIs.
8. JWT security protection.
9. Full verification.

## 10. Design Conclusion

Phase 4 makes the backend administrable. It closes the RBAC management loop without overbuilding a full policy engine. After this phase, the frontend can build system management pages, initialize user menus and permissions after login, and rely on JWT protection for normal business APIs.

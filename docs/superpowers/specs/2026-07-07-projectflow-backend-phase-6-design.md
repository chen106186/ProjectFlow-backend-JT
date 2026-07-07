# ProjectFlow Backend Phase 6 Design

## Overview

Phase 6 turns the RBAC data model from a management feature into an active API authorization layer. Phases 4 and 5 added role, menu, permission assignment, JWT protection, paginated system APIs, and admin seed data. This phase loads a user's assigned permission codes into Spring Security and uses method-level authorization for system management and core business write operations.

The goal is not to build a full data-permission engine. The first authorization pass should be predictable, easy for the frontend to understand, and aligned with the existing `sys_menu` and `sys_role_menu` tables.

## Current State

The backend currently has:

- JWT authentication for all non-public APIs.
- `CurrentUserContext` populated from JWT user id.
- `CurrentUserPermissionService` that can return roles, menus, and permission codes for the current user.
- Editable departments, users, roles, menus, and role-menu assignments.
- Seeded admin role, admin user, and system menu entries.

The missing piece is enforcement. `SecurityConfig` only requires authentication, and controllers do not currently declare permission requirements.

## Recommended Approach

Use Spring Security method authorization with `@PreAuthorize("hasAuthority('permission:code')")`.

JWT authentication will still identify the user. After token validation, the filter will query the user's current permission codes and place them into `UsernamePasswordAuthenticationToken` as `GrantedAuthority` values. Controllers can then protect methods using permission codes that match the codes returned by `/api/system/me`.

This keeps the backend and frontend on one permission vocabulary:

- Backend uses the code for enforcement.
- Frontend uses the same code for menu and button visibility.
- Admin can manage codes through the existing menu management APIs.

## Permission Model

Phase 6 uses two permission categories:

1. Menu permissions: page visibility, already represented by `type = MENU`.
2. Button/API permissions: action permissions represented by `type = BUTTON`.

Permission code format:

- System management: `system:user:create`, `system:role:assign-menu`, `system:log:view`.
- Project workflow: `project:create`, `task:update`, `bug:assign`, `requirement:status`.
- Read-only business list and detail APIs remain authenticated but not method-gated in Phase 6.

The admin seed migration will add the Phase 6 button/API permissions and assign them to the existing admin role id `1000000000000000101`.

## Protected API Scope

Phase 6 protects:

- System logs, users, departments, roles, menus, and role-menu assignment.
- Project create, update, delete, and gantt node update.
- Task create, update, delete, and actual-time update.
- Requirement create, update, and status update.
- BUG create, update, assign, close, and comment create.

Phase 6 does not protect:

- `/api/auth/login`.
- Swagger and OpenAPI endpoints.
- `/api/system/me` and `/api/system/me/permissions`.
- Business read-only list/detail APIs beyond requiring login.
- `/my` endpoints beyond requiring login.

## Security Flow

1. Client sends `Authorization: Bearer <token>`.
2. `JwtAuthenticationFilter` parses user id.
3. A new permission lookup component loads current permission codes for that user id.
4. The filter builds `UsernamePasswordAuthenticationToken` with `SimpleGrantedAuthority` values.
5. `@PreAuthorize` expressions decide whether the method may run.
6. Access denied responses return the existing `ApiResponse` JSON shape with `403` and the request trace id.

If the token is invalid, the current behavior remains: no authentication is installed and protected APIs return `401`.

## Components

### Current User Permission Lookup

Add a user-id based permission lookup method instead of relying on `CurrentUserContext` inside the filter. The filter runs before controllers and should not need to call the current-user profile method.

The lookup should:

- Return an empty list if no roles or no menus are assigned.
- Include both MENU and BUTTON codes.
- Preserve stable ordering.
- Deduplicate codes.
- Reject disabled or missing users through the normal authentication path by leaving the request unauthenticated or surfacing `401`.

### Security Configuration

Enable method security with `@EnableMethodSecurity`.

Configure:

- JSON `401` response for unauthenticated requests.
- JSON `403` response for authenticated requests without required permission.
- JWT filter constructed with both `JwtTokenService` and the permission lookup service.

### Controllers

Use `@PreAuthorize` directly on controller methods. This is intentionally explicit and close to the API contract.

Examples:

- `@PreAuthorize("hasAuthority('system:user:view')")`
- `@PreAuthorize("hasAuthority('system:user:create')")`
- `@PreAuthorize("hasAuthority('project:update')")`

For Phase 6, list/detail reads in the system module are protected because they expose administration data. Business list/detail reads remain authenticated-only.

## Database Migration

Add `V6__phase_6_permission_seed.sql`.

The migration will:

- Add system button permissions under their menu parents.
- Add business top-level menu rows if missing, so frontend menu construction has stable roots.
- Add business action permissions under the relevant menu roots.
- Assign all Phase 6 permissions to the seeded admin role.

The migration must be idempotent using `INSERT ... SELECT ... WHERE NOT EXISTS`.

## Error Handling

Authorization failures should use the existing response envelope:

```json
{
  "code": 403,
  "message": "Forbidden",
  "data": null,
  "traceId": "..."
}
```

Authentication failures should use the same envelope with `401`.

## Testing

Phase 6 tests should cover:

- Permission lookup returns deduplicated permission codes.
- JWT-authenticated requests receive authorities from the database-backed lookup.
- Missing permission returns `403`.
- Existing anonymous access rules still permit login and OpenAPI.
- Controller methods compile with method-level security enabled.
- Migration is validated by starting the application against the local MySQL schema.

## Out of Scope

Phase 6 does not implement:

- Department, project member, creator, assignee, or project-manager data permissions.
- Permission caching in Redis.
- Permission expression DSL beyond `hasAuthority`.
- Role hierarchy.
- Frontend changes.

These are good later phases after the frontend starts consuming the permission codes.

## Definition of Done

- JWT authentication installs granted authorities from RBAC assignments.
- System management APIs require explicit `system:*` permissions.
- Core business write APIs require explicit action permissions.
- Admin seed data includes all Phase 6 permission codes.
- Unauthorized requests return `401` JSON and insufficient permissions return `403` JSON.
- Full tests, package build, and local OpenAPI startup verification pass.

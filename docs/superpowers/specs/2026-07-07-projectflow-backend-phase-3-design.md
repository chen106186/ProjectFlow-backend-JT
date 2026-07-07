# ProjectFlow Backend Phase 3 Design

## 1. Goal

Phase 3 turns the remaining P0 API skeletons into usable backend workflows. Phase 1 created the backend foundation. Phase 2 added requirement, Gantt, notice, export, and statistics workflows. Phase 3 focuses on core daily operations that are still incomplete:

1. Project list and project detail CRUD.
2. Task list, my tasks, task detail, and task edit workflow.
3. BUG list, my BUGs, BUG detail, comments, assignment, and close workflow.
4. File upload and download APIs using the existing `FileStorageService`.
5. Basic system user, role, and menu query APIs for frontend selectors and management pages.

The phase stays on the existing `codex/projectflow-phase-1` branch and continues the modular monolith architecture.

## 2. Current Gaps

Current state after Phase 2:

- `ProjectController.listProjects()` returns an empty list.
- `TaskController.listTasks()` returns an empty list.
- `BugController.listBugs()` returns an empty list.
- File storage services exist, but no file upload/download controller exists.
- System user mapper exists for login, but management APIs are missing.

## 3. Scope

### In Scope

- Project create, update, detail, list, and soft delete.
- Project list filtering by type, status, contract status, manager, and keyword.
- Task create, update, detail, list, my tasks, and soft delete.
- Task status recalculation after actual start/end changes.
- Task operation logs for create, update, status change, and delete.
- BUG create, update, detail, list, my BUGs, assign, close, and comment.
- BUG operation logs for create, status change, assignment, close, and comment.
- File upload, list, download, and soft delete by business type and business id.
- Basic user list, department list, role list, and menu tree APIs.
- DTOs designed for frontend tables, drawers, and detail pages.

### Out of Scope

- Frontend implementation.
- Advanced workflow engine.
- WebSocket real-time push.
- Complex department tree editing.
- Password reset flow.
- Bulk import.
- Rich file preview.

## 4. Module Design

### project

Add `ProjectEntity`, `ProjectMapper`, `ProjectService`, request DTOs, response DTOs, and real controller endpoints.

APIs:

```text
POST   /api/projects
GET    /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

Rules:

- Project type is `MANAGEMENT` or `EXECUTION`.
- Project status defaults to `NOT_STARTED`.
- Delete is logical delete.
- Create, update, and delete write operation logs.

### task

Add request/response DTOs and service for task operations.

APIs:

```text
POST   /api/tasks
GET    /api/tasks
GET    /api/tasks/my
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
PATCH  /api/tasks/{id}/actual-time
```

Rules:

- Task create requires project id, name, assignee, and priority.
- Status is calculated by `TaskStatusCalculator`.
- `my` filters by current user as assignee.
- Actual start/end updates write operation logs.
- Delete is logical delete.

### bug

Add service and DTOs for BUG operations.

APIs:

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

Rules:

- BUG create requires title, project id, assignee, description, and reproduce steps.
- My BUGs include bugs created by or assigned to current user.
- Only creator or assignee can edit operational fields.
- Assign and close write operation logs.
- Comments persist in `pf_bug_comment` and write operation logs.

### file

Add file metadata service and controller.

APIs:

```text
POST   /api/files
GET    /api/files
GET    /api/files/{id}/download
DELETE /api/files/{id}
```

Rules:

- Upload accepts `businessType`, `businessId`, `versionNo`, and multipart file.
- File size max remains 50 MB from application configuration.
- Metadata is saved to `pf_file`.
- Delete is logical delete and also calls storage delete when safe.

### system

Add lightweight management query APIs.

APIs:

```text
GET /api/system/users
GET /api/system/departments
GET /api/system/roles
GET /api/system/menus
```

Rules:

- These APIs support frontend selectors and table pages.
- Full role permission editing belongs to Phase 4 permission administration.

## 5. Data Changes

Add a Phase 3 migration:

```text
V3__phase_3_business_indexes.sql
```

Expected changes:

- Index project keyword/status filters.
- Index task project/assignee/status filters.
- Index BUG project/status/assignee filters.
- Index file business lookup.
- Index user department/enabled lookup.

No destructive schema changes.

## 6. Testing Strategy

Priority tests:

- Project create/list/update service tests.
- Task actual time update recalculates status.
- My tasks filters by assignee.
- BUG assign and close update status and logs.
- BUG comments are listed by time.
- File upload persists metadata and delegates storage.
- User list filters by enabled and department.

Use service-level tests first, then controller smoke tests where routing risk is high.

## 7. Delivery Order

1. Phase 3 database indexes.
2. Project CRUD.
3. Task workflow.
4. BUG workflow and comments.
5. File upload/download APIs.
6. System query APIs.
7. Full verification.

## 8. Design Conclusion

Phase 3 completes the practical P0 backend surface. After this phase, frontend pages for project list, task list, BUG list, file attachments, and basic system selectors can call real APIs instead of skeleton responses.

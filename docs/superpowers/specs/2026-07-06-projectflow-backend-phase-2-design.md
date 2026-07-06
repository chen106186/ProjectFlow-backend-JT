# ProjectFlow Backend Phase 2 Design

## 1. Goal

Phase 2 turns the Phase 1 backend foundation into a stronger business workflow backend. The scope focuses on five capabilities:

1. Requirement management full workflow.
2. Project Gantt data and project node progress calculation.
3. Notification center.
4. Excel exports for operational lists.
5. Personal statistics and dashboard enhancement.

The work stays in the existing modular monolith and the existing branch. It should build on Phase 1 tables and package boundaries instead of introducing a new service or repository.

## 2. Scope

### In Scope

- Requirement create, edit, detail, list, grouping, and status transitions.
- Requirement operation logs for create and status or content changes.
- Project node Gantt data API with progress, overdue, due-soon, and timeline fields.
- Project node update operations with operation logs.
- Notification storage and read/unread operations.
- Notification creation for task assignment, BUG assignment, BUG comment, project warning, and requirement status changes.
- Excel export endpoints for operation logs, task list, requirement list, BUG list, and Gantt node data.
- Personal statistics endpoints for current user's tasks, BUGs, requirements, and completion/overdue counts.
- Dashboard summary extension using existing task, project, BUG, requirement, and notice data.

### Out of Scope

- Frontend implementation.
- Real-time WebSocket notification push.
- Complex workflow engine integration.
- Microservice splitting.
- Advanced permission UI editing beyond the existing RBAC foundation.
- Full production Aliyun OSS hardening beyond the Phase 1 storage adapter.

## 3. Module Responsibilities

### requirement

Owns requirement business workflow.

Core status values:

```text
PENDING_REVIEW
ACCEPTED
REJECTED
```

Rules:

- New requirements default to `PENDING_REVIEW`.
- Only permitted users can move a requirement to `ACCEPTED` or `REJECTED`.
- Any create, edit, or status transition writes an operation log.
- My requirements list filters by creator.
- Requirement list supports project, type, priority, status, keyword, and grouping fields.

### project

Owns Gantt node data and project progress.

Rules:

- Gantt nodes are read from `pf_project_node`.
- Node progress is calculated from explicit `progress_percent`, actual dates, and node status.
- A node is overdue when it has no actual end date and today is after planned end date.
- A node is due soon when it has no actual end date and planned end date is within 7 days.
- Updating node dates or progress writes an operation log.

### notice

Owns notification records.

Notification types:

```text
TASK_ASSIGNED
BUG_ASSIGNED
BUG_COMMENT
PROJECT_WARNING
REQUIREMENT_STATUS_CHANGED
SYSTEM
```

Rules:

- Notifications are persisted in `pf_notice`.
- Read/unread is controlled by `read_flag` and `read_at`.
- Mark-one-read and mark-all-read are supported.
- Notifications include `business_type` and `business_id` for frontend navigation.

### export

Use a new package under `common` or a focused module:

```text
com.jitong.projectflow.export
```

Rules:

- Export endpoints return an Excel file stream.
- Exports use EasyExcel.
- Export queries reuse service-layer query objects instead of duplicating mapper logic in controllers.
- Operation log export includes username, module, operation type, content, and created time.

### dashboard

Extends Phase 1 summary endpoints.

Rules:

- Personal statistics use current user context.
- Dashboard todo ordering follows the existing `TodoSortKey`.
- Notice unread count is included in dashboard summary.

## 4. API Design

### Requirement APIs

```text
POST   /api/requirements
GET    /api/requirements
GET    /api/requirements/{id}
PUT    /api/requirements/{id}
PATCH  /api/requirements/{id}/status
GET    /api/requirements/my
```

### Gantt APIs

```text
GET    /api/projects/{projectId}/gantt
PATCH  /api/projects/{projectId}/nodes/{nodeId}
GET    /api/projects/{projectId}/gantt/summary
```

### Notice APIs

```text
GET    /api/notices
GET    /api/notices/unread-count
PATCH  /api/notices/{id}/read
PATCH  /api/notices/read-all
```

### Export APIs

```text
GET    /api/exports/operation-logs
GET    /api/exports/tasks
GET    /api/exports/requirements
GET    /api/exports/bugs
GET    /api/exports/projects/{projectId}/gantt
```

### Statistics APIs

```text
GET    /api/dashboard/my-statistics
GET    /api/dashboard/summary
GET    /api/dashboard/todos
```

## 5. Data Changes

Phase 2 should add one migration:

```text
V2__phase_2_workflow_indexes.sql
```

Expected changes:

- Add useful indexes for requirement filters.
- Add useful indexes for project node timeline queries.
- Add useful indexes for notice inbox queries.
- Add useful indexes for operation log export filters.

No destructive table changes should be made. Existing Phase 1 tables remain the source of truth.

## 6. Error Handling

Use existing `BusinessException` and `ErrorCode`.

Important error cases:

- Requirement not found.
- Invalid requirement status transition.
- Project node not found or does not belong to project.
- Notice not found or does not belong to current user.
- Export query too broad if no filter is provided for large data.

## 7. Testing Strategy

Priority tests:

- Requirement status transition tests.
- Requirement create/edit operation log tests.
- Gantt node summary calculation tests.
- Notice mark-read and unread-count tests.
- Todo/statistics aggregation tests.
- Export service tests that verify generated rows and headers using an in-memory output stream.

Controller tests can be added after service tests for key endpoints.

## 8. Delivery Order

1. Requirement workflow.
2. Gantt node APIs and summary calculation.
3. Notification center.
4. Excel exports.
5. Personal statistics and dashboard enhancement.
6. Full verification and Swagger check.

## 9. Design Conclusion

Phase 2 remains a modular-monolith extension. It strengthens the business workflow without adding infrastructure risk. Requirement management, Gantt data, and notifications form the primary business loop; exports and personal statistics provide management and reporting value after the core loop is stable.

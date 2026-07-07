# ProjectFlow Backend Phase 9 Design

## Overview

Phase 9 closes two backend gaps from the Word requirements: daily reports and project reports. The frontend is out of scope; this phase provides stable REST APIs, persistence, pagination, filtering, operation logs, and RBAC permission codes that a future UI can consume.

## Scope

- Daily report CRUD and query APIs.
- Project report CRUD and query APIs.
- Project report preparation-item CRUD inside the report aggregate.
- Operation logs for create, update, delete, status change, and preparation-item changes.
- RBAC permission codes and admin seed assignments.
- File attachments remain linked through the existing file module by `businessType` and `businessId`.

## Out of Scope

- Frontend pages, calendars, drawers, rich text editors, and upload progress UI.
- A new storage provider or MinIO implementation.
- Scheduled report reminders.
- Data-level role range rules beyond authenticated/RBAC-gated APIs.

## Daily Reports

Daily reports are stored in `pf_daily_report`. A report belongs to one project and one report date. The reporter is the current authenticated user on creation. The list API supports project, reporter, date range, and keyword filters, returning `PageResult`.

Endpoints:

- `POST /api/daily-reports`
- `GET /api/daily-reports`
- `GET /api/daily-reports/my`
- `GET /api/daily-reports/{id}`
- `PUT /api/daily-reports/{id}`
- `DELETE /api/daily-reports/{id}`

## Project Reports

Project reports are stored in `pf_project_report`. Preparation items are stored in `pf_project_report_item`. A project report belongs to a project, has a type, status, planned date, optional actual date, target audience, location or method, and description.

Report status values are simple strings to match the existing codebase style: `PREPARING`, `UPCOMING`, `COMPLETED`, `CANCELLED`.

Endpoints:

- `POST /api/project-reports`
- `GET /api/project-reports`
- `GET /api/project-reports/{id}`
- `PUT /api/project-reports/{id}`
- `PATCH /api/project-reports/{id}/status`
- `DELETE /api/project-reports/{id}`
- `POST /api/project-reports/{id}/items`
- `PUT /api/project-reports/{id}/items/{itemId}`
- `DELETE /api/project-reports/{id}/items/{itemId}`

## Permissions

New permission codes:

- `daily-report:create`
- `daily-report:update`
- `project-report:create`
- `project-report:update`

Read APIs remain authenticated-only. Write APIs require the corresponding permission code. The seeded admin role receives all new permissions.

## Testing

Service tests cover creation defaults, pagination filters, current-user ownership fields, status updates, item CRUD, and operation logging. Startup verification ensures Flyway applies the new migration and OpenAPI exposes the new controllers.

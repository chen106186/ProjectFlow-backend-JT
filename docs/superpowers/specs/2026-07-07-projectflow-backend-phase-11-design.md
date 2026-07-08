# ProjectFlow Backend Phase 11 Design

## Overview

Phase 11 adds data-level authorization on top of the existing RBAC permission codes. Controllers still use `@PreAuthorize` for coarse API permissions. Services now verify whether the current user can modify the specific business object.

## Scope

- Add `BusinessAccessService`.
- Protect project update/delete by project manager, creator, or system admin.
- Protect task update/delete by project manager, creator, or system admin.
- Allow task actual-time updates by the task assignee, project manager, creator, or system admin.
- Protect Bug update/assign/comment by creator, assignee, or system admin.
- Protect Bug close by creator or system admin.
- Protect file delete by uploader or system admin.
- Protect daily report update/delete by reporter, creator, or system admin.
- Protect project report update/delete/item mutation by project manager, creator, or system admin.

## System Admin Rule

For this phase, a user is treated as a system admin when the authenticated authorities contain `system:user:update` or `system:role:update`. This uses existing seeded permissions and avoids introducing a new role model.

## Error Handling

Data authorization failures throw `BusinessException(ErrorCode.FORBIDDEN, "无权操作该数据")`.

## Out of Scope

- Read-side data filtering.
- Department tree authorization.
- New permission seed data.
- Frontend changes.

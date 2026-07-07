# ProjectFlow Backend Phase 8 Design

## Overview

Phase 8 hardens the notification center for frontend integration. The current notice module can create notices, list a user's notices, count unread notices, mark one notice as read, and mark all notices as read. That is enough for early backend behavior, but the frontend notification page needs predictable pagination and filters.

This phase keeps notifications as authenticated personal data. It does not add push delivery, WebSocket, email, SMS, or scheduled jobs.

## Goals

- Return paginated notice lists.
- Filter notices by read state, notice type, business type, and business id.
- Preserve unread-count, mark-read, and mark-all-read behavior.
- Keep notice APIs authenticated-only.
- Replace garbled notice-not-found messages with stable English text.

## API Contract

`GET /api/notices` returns `ApiResponse<PageResult<NoticeResponse>>`.

Supported query parameters:

- `pageNo`, default `1`.
- `pageSize`, default `20`, max `200`.
- `read`: optional `true` or `false`.
- `noticeType`: optional exact notice type.
- `businessType`: optional exact business type.
- `businessId`: optional exact business id.

Sorting stays newest first by `createdAt`.

Existing endpoints stay unchanged:

- `GET /api/notices/unread-count`
- `PATCH /api/notices/read-all`
- `PATCH /api/notices/{id}/read`

## Service Design

Create `NoticeQueryRequest` extending `PageQuery`.

`NoticeService.list(Long receiverId, NoticeQueryRequest request)` will:

1. Build a MyBatis-Plus `LambdaQueryWrapper`.
2. Always filter by `receiverId`.
3. Add optional filters from the request.
4. Execute paginated query with `PageUtils.toPage`.
5. Return `PageResult<NoticeResponse>`.

Existing internal callers and tests should use the new request object. A small helper method in tests can build a default request.

## Error Handling

`markRead` returns `BusinessException(ErrorCode.NOT_FOUND, "Notice not found")` when the notice does not exist or belongs to a different user.

Validation failures continue to use the global `400` response.

## Permissions

Notice endpoints remain authenticated-only in Phase 8. A user's receiver id comes from `CurrentUserContext`, so users cannot query another user's notices through the API.

## Out of Scope

Phase 8 does not implement:

- WebSocket push.
- Email/SMS integration.
- Scheduled overdue notification generation.
- Notification deletion.
- Admin notice broadcast.
- Cross-user notice management.

## Definition of Done

- Notice list returns `PageResult`.
- Notice list supports read/type/business filters.
- Existing unread and mark-read workflows still pass.
- Notice-not-found message is stable.
- Full tests, package build, and local OpenAPI startup verification pass.

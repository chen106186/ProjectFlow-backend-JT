# ProjectFlow Backend Phase 8 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make notification center APIs frontend-ready with paginated notice lists, filters, and stable error messages.

**Architecture:** Reuse existing notice module and shared pagination helpers. Add a request DTO extending `PageQuery`, return `PageResult<NoticeResponse>` from list APIs, and keep personal notice access scoped by `CurrentUserContext`.

**Tech Stack:** Java 21, Spring Boot 3.5.x, MyBatis-Plus, MySQL 8, JUnit 5, H2-backed Spring Boot tests.

---

## Task 1: Paginated Notice Query

**Files:**
- Create: `src/main/java/com/jitong/projectflow/notice/dto/NoticeQueryRequest.java`
- Modify: `src/main/java/com/jitong/projectflow/notice/service/NoticeService.java`
- Modify: `src/main/java/com/jitong/projectflow/notice/controller/NoticeController.java`
- Test: `src/test/java/com/jitong/projectflow/notice/service/NoticeServiceTest.java`

- [x] **Step 1: Add query DTO**

Create `NoticeQueryRequest extends PageQuery` with:

- `Boolean read`
- `String noticeType`
- `String businessType`
- `Long businessId`

- [x] **Step 2: Update service tests for `PageResult`**

Update existing tests that call `noticeService.list(RECEIVER_ID)` to use a default `NoticeQueryRequest`.

Add tests for:

- `list` returns total and records.
- `read=false` returns only unread records.
- `noticeType` filters exact notice type.
- `businessType` and `businessId` filter exact business object.

- [x] **Step 3: Implement paginated service list**

Change `NoticeService.list(Long receiverId, NoticeQueryRequest request)` to return `PageResult<NoticeResponse>`.

Rules:

- Always filter by receiver id.
- Add optional filters when request fields are present.
- Order by `createdAt DESC`.
- Use `PageUtils.toPage` and `PageUtils.toResult`.

- [x] **Step 4: Update controller contract**

Change `GET /api/notices` to accept `@Valid @ModelAttribute NoticeQueryRequest` and return `ApiResponse<PageResult<NoticeResponse>>`.

- [x] **Step 5: Verify**

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=NoticeServiceTest test
```

- [x] **Step 6: Commit**

```bash
git add src/main/java/com/jitong/projectflow/notice src/test/java/com/jitong/projectflow/notice docs/superpowers/plans/2026-07-07-projectflow-backend-phase-8.md
git commit -m "feat: paginate notice list"
```

## Task 2: Stable Notice Error Message

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/notice/service/NoticeService.java`
- Test: `src/test/java/com/jitong/projectflow/notice/service/NoticeServiceTest.java`

- [x] **Step 1: Update test expectation**

Change wrong-receiver mark-read test to expect message `Notice not found`.

- [x] **Step 2: Update service error message**

Change `markRead` not-found message to `Notice not found`.

- [x] **Step 3: Verify**

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=NoticeServiceTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/notice src/test/java/com/jitong/projectflow/notice docs/superpowers/plans/2026-07-07-projectflow-backend-phase-8.md
git commit -m "fix: stabilize notice error message"
```

## Task 3: Phase 8 Verification

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
git add docs/superpowers/plans/2026-07-07-projectflow-backend-phase-8.md
git commit -m "docs: mark phase 8 verification complete"
```

## Definition of Done

- `GET /api/notices` returns `PageResult`.
- Notice list supports read/type/business filters.
- Existing unread count and mark-read flows still pass.
- Notice not found message is stable.
- Full tests and package build pass.
- Application starts and OpenAPI exposes Phase 8 contract.

# ProjectFlow Backend Phase 7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden file attachment APIs for frontend integration with upload constraints, automatic versions, business-object scoped lists, and RBAC write permissions.

**Architecture:** Keep the existing `FileStorageService` abstraction and `pf_file` metadata table. Add validation and version selection in `FileService`, add method security on file write endpoints, and seed file permission rows through Flyway.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Security method authorization, MyBatis-Plus, MySQL 8, Flyway, JUnit 5, Mockito, MockMvc.

---

## Task 1: Upload Validation and Automatic Versions

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/file/service/FileService.java`
- Test: `src/test/java/com/jitong/projectflow/file/service/FileServiceTest.java`

- [x] **Step 1: Add upload validation tests**

Add tests for:

- Unsupported extension throws `BusinessException` with message `File type not allowed`.
- File larger than 50 MB throws `BusinessException` with message `File size exceeds 50 MB`.
- Blank original filename throws `BusinessException` with message `File name is required`.

- [x] **Step 2: Add auto-version tests**

Add tests for:

- Blank `versionNo` with no existing same-name files persists `v1`.
- Blank `versionNo` with existing `v1`, `v3`, and `draft` persists `v4`.
- Explicit `versionNo` still persists the provided value.

- [x] **Step 3: Implement validation and version selection**

Rules:

- Allowed extensions: `docx`, `xlsx`, `pdf`, `png`, `jpg`, `jpeg`, `drawio`.
- Max file size: `50 * 1024 * 1024`.
- Require nonblank original filename.
- If `versionNo` is blank, query by same `businessType`, `businessId`, and `originalName`, then use next numeric `vN`.

- [x] **Step 4: Verify**

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=FileServiceTest test
```

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/file src/test/java/com/jitong/projectflow/file docs/superpowers/plans/2026-07-07-projectflow-backend-phase-7.md
git commit -m "feat: harden file upload versions"
```

## Task 2: Business-Scoped File Lists

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/file/service/FileService.java`
- Test: `src/test/java/com/jitong/projectflow/file/service/FileServiceTest.java`

- [x] **Step 1: Add list validation tests**

Add tests that verify `list(null, 1L)` and `list("TASK", null)` throw `BusinessException` with message `File query requires businessType and businessId`.

- [x] **Step 2: Implement strict list filters**

Change `FileService.list` so both filters are required and always applied.

- [x] **Step 3: Verify**

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=FileServiceTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/file src/test/java/com/jitong/projectflow/file docs/superpowers/plans/2026-07-07-projectflow-backend-phase-7.md
git commit -m "feat: require scoped file lists"
```

## Task 3: File API Permissions

**Files:**
- Modify: `src/main/java/com/jitong/projectflow/file/controller/FileController.java`
- Modify: `src/test/java/com/jitong/projectflow/auth/security/SecurityConfigTest.java`

- [x] **Step 1: Add security tests**

Add MockMvc tests for:

- Authenticated list request to `/api/files?businessType=TASK&businessId=1` succeeds without file action permissions.
- Upload without `file:upload` returns `403`.
- Delete without `file:delete` returns `403`.

- [x] **Step 2: Add `@PreAuthorize` annotations**

Apply:

- `POST /api/files` requires `file:upload`.
- `DELETE /api/files/{id}` requires `file:delete`.

Leave list and download authenticated-only.

- [x] **Step 3: Verify**

```bash
mvn -q "-Dmaven.repo.local=.m2/repository" -Dtest=SecurityConfigTest test
```

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/jitong/projectflow/file src/test/java/com/jitong/projectflow/auth docs/superpowers/plans/2026-07-07-projectflow-backend-phase-7.md
git commit -m "feat: protect file write apis"
```

## Task 4: Phase 7 File Permission Seed

**Files:**
- Create: `src/main/resources/db/migration/V7__phase_7_file_permissions.sql`

- [ ] **Step 1: Add permission seed migration**

Seed:

```text
file
file:upload
file:delete
```

Assign all three to admin role `1000000000000000101`.

- [ ] **Step 2: Verify migration file exists**

```bash
rg --files src/main/resources/db/migration
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V7__phase_7_file_permissions.sql docs/superpowers/plans/2026-07-07-projectflow-backend-phase-7.md
git commit -m "feat: seed file permissions"
```

## Task 5: Phase 7 Verification

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
git add docs/superpowers/plans/2026-07-07-projectflow-backend-phase-7.md
git commit -m "docs: mark phase 7 verification complete"
```

## Definition of Done

- Upload rejects unsupported types, files over 50 MB, and blank names.
- Blank upload version numbers are generated as the next `vN`.
- File list requires `businessType` and `businessId`.
- File upload and delete require file permissions.
- Admin role receives file permissions through Flyway.
- Full tests and package build pass.
- Application starts and OpenAPI exposes Phase 7 contracts.

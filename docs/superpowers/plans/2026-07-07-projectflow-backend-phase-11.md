# ProjectFlow Backend Phase 11 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add data-level authorization checks for core business write operations.

**Architecture:** Introduce `BusinessAccessService` as a small authorization policy service. Existing module services call it after loading the target entity and before mutating data.

**Tech Stack:** Java 21, Spring Boot Security, MyBatis-Plus, JUnit 5, Mockito.

---

### Task 1: Business Access Service

**Files:**
- Create: `src/main/java/com/jitong/projectflow/auth/security/BusinessAccessService.java`
- Create: `src/test/java/com/jitong/projectflow/auth/security/BusinessAccessServiceTest.java`

- [ ] Write failing tests for system-admin, project manager, task assignee actual-time access, Bug creator/assignee access, file uploader delete, and forbidden cases.
- [ ] Implement authority lookup from `SecurityContextHolder` and current-user id from `CurrentUserContext`.
- [ ] Throw `BusinessException(FORBIDDEN)` with message `无权操作该数据`.

### Task 2: Service Integration

**Files:**
- Modify: business services for project, task, bug, file, daily report, and project report.
- Modify: affected service tests.

- [ ] Inject `BusinessAccessService`.
- [ ] Call the correct check immediately after loading entities.
- [ ] Keep existing RBAC annotations unchanged except task actual-time may be authenticated-only if needed later.

### Task 3: Verification

**Commands:**
- `$env:JAVA_HOME='D:\devTool\jdk21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; mvn "-Dmaven.repo.local=.m2/repository" test`
- `$env:JAVA_HOME='D:\devTool\jdk21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; mvn "-Dmaven.repo.local=.m2/repository" package`

- [ ] Run full tests.
- [ ] Run package build.

# ProjectFlow Backend Phase 9 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement backend APIs for daily reports and project reports.

**Architecture:** Add two bounded modules, `daily` and `report`, following the existing controller/service/mapper/entity/dto pattern. Store attachments through the existing file module by business object instead of adding file columns.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL 8, JUnit 5, Mockito.

---

### Task 1: Database And Permissions

**Files:**
- Create: `src/main/resources/db/migration/V8__phase_9_daily_project_reports.sql`

- [ ] Add tables `pf_daily_report`, `pf_project_report`, and `pf_project_report_item`.
- [ ] Add indexes for report date, project, reporter, status, and report items.
- [ ] Seed menu permissions for `daily-report:create`, `daily-report:update`, `project-report:create`, and `project-report:update`.
- [ ] Assign all new permissions to the seeded admin role.

### Task 2: Daily Report Module

**Files:**
- Create package: `src/main/java/com/jitong/projectflow/daily`
- Create tests: `src/test/java/com/jitong/projectflow/daily/service/DailyReportServiceTest.java`

- [ ] Write failing service tests for create, list filtering, my reports, update, and delete logging.
- [ ] Add entity, mapper, DTOs, service, and controller.
- [ ] Protect write endpoints with daily report permissions.
- [ ] Return paginated list responses for list APIs.

### Task 3: Project Report Module

**Files:**
- Create package: `src/main/java/com/jitong/projectflow/report`
- Create tests: `src/test/java/com/jitong/projectflow/report/service/ProjectReportServiceTest.java`

- [ ] Write failing service tests for create, list filtering, status update, item create/update/delete, and logging.
- [ ] Add entity, item entity, mappers, DTOs, service, and controller.
- [ ] Protect write endpoints with project report permissions.
- [ ] Return report details with preparation items.

### Task 4: Verification

**Commands:**
- `$env:JAVA_HOME='D:\devTool\jdk21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; mvn "-Dmaven.repo.local=.m2/repository" test`
- `$env:JAVA_HOME='D:\devTool\jdk21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; mvn "-Dmaven.repo.local=.m2/repository" package`

- [ ] Run the full test suite.
- [ ] Build the package.
- [ ] Fix any failures using failing tests first.

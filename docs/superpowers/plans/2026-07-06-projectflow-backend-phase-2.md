# ProjectFlow Backend Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 2 backend workflows for requirements, Gantt data, notifications, Excel exports, and personal statistics.

**Architecture:** Continue the existing modular monolith on the current branch. Add focused services and DTOs under existing modules, and avoid introducing new infrastructure or new branches.

**Tech Stack:** Java 21, Spring Boot 3.5.x, MySQL 8, MyBatis-Plus, Flyway, EasyExcel, JUnit 5.

---

## Task 1: Phase 2 Database Index Migration

**Files:**
- Create: `src/main/resources/db/migration/V2__phase_2_workflow_indexes.sql`

- [x] **Step 1: Add migration**

Create `V2__phase_2_workflow_indexes.sql`:

```sql
CREATE INDEX idx_pf_requirement_status_priority ON pf_requirement (status, priority);
CREATE INDEX idx_pf_requirement_project_status ON pf_requirement (project_id, status);
CREATE INDEX idx_pf_requirement_created_time ON pf_requirement (created_by, created_at);

CREATE INDEX idx_pf_project_node_status_dates ON pf_project_node (project_id, status, planned_end_date);
CREATE INDEX idx_pf_project_node_parent ON pf_project_node (project_id, parent_id);

CREATE INDEX idx_pf_notice_receiver_time ON pf_notice (receiver_id, created_at);
CREATE INDEX idx_pf_notice_type_read ON pf_notice (notice_type, read_flag);

CREATE INDEX idx_sys_operation_log_module_time ON sys_operation_log (module, created_at);
CREATE INDEX idx_sys_operation_log_type_time ON sys_operation_log (operation_type, created_at);
```

- [x] **Step 2: Verify migration naming**

Run:

```bash
rg --files src/main/resources/db/migration
```

Expected: both `V1__init_schema.sql` and `V2__phase_2_workflow_indexes.sql` are present.

- [x] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V2__phase_2_workflow_indexes.sql
git commit -m "feat: add phase 2 workflow indexes"
```

## Task 2: Requirement Workflow

**Files:**
- Create: `src/main/java/com/jitong/projectflow/requirement/domain/RequirementStatus.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/domain/RequirementPriority.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/domain/RequirementStatusPolicy.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/entity/RequirementEntity.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/mapper/RequirementMapper.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/dto/RequirementCreateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/dto/RequirementUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/dto/RequirementStatusUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/dto/RequirementResponse.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/service/RequirementService.java`
- Modify: `src/main/java/com/jitong/projectflow/requirement/controller/RequirementController.java`
- Test: `src/test/java/com/jitong/projectflow/requirement/domain/RequirementStatusPolicyTest.java`

- [x] **Step 1: Write failing status policy tests**

Create `RequirementStatusPolicyTest.java`:

```java
package com.jitong.projectflow.requirement.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementStatusPolicyTest {
    private final RequirementStatusPolicy policy = new RequirementStatusPolicy();

    @Test
    void pendingRequirementCanBeAccepted() {
        assertThat(policy.canTransition(RequirementStatus.PENDING_REVIEW, RequirementStatus.ACCEPTED)).isTrue();
    }

    @Test
    void acceptedRequirementCannotMoveBackToPending() {
        assertThat(policy.canTransition(RequirementStatus.ACCEPTED, RequirementStatus.PENDING_REVIEW)).isFalse();
    }
}
```

- [x] **Step 2: Implement status enums and policy**

```java
package com.jitong.projectflow.requirement.domain;

public enum RequirementStatus {
    PENDING_REVIEW,
    ACCEPTED,
    REJECTED
}
```

```java
package com.jitong.projectflow.requirement.domain;

public enum RequirementPriority {
    URGENT,
    HIGH,
    MEDIUM,
    LOW
}
```

```java
package com.jitong.projectflow.requirement.domain;

public class RequirementStatusPolicy {
    public boolean canTransition(RequirementStatus from, RequirementStatus to) {
        if (from == to) {
            return true;
        }
        return from == RequirementStatus.PENDING_REVIEW
                && (to == RequirementStatus.ACCEPTED || to == RequirementStatus.REJECTED);
    }
}
```

- [x] **Step 3: Implement entity, mapper, DTOs, service, and controller**

Implement CRUD endpoints from the Phase 2 design:

```text
POST   /api/requirements
GET    /api/requirements
GET    /api/requirements/{id}
PUT    /api/requirements/{id}
PATCH  /api/requirements/{id}/status
GET    /api/requirements/my
```

Service requirements:

- New records default to `PENDING_REVIEW`.
- Status transition uses `RequirementStatusPolicy`.
- Not found throws `BusinessException(ErrorCode.NOT_FOUND, "Requirement not found")`.
- Invalid transition throws `BusinessException(ErrorCode.CONFLICT, "Invalid requirement status transition")`.
- Create, update, and status change call `OperationLogService.record(...)`.

- [x] **Step 4: Verify**

Run:

```bash
mvn -q -Dtest=RequirementStatusPolicyTest test
```

Expected: tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/requirement src/test/java/com/jitong/projectflow/requirement
git commit -m "feat: implement requirement workflow"
```

## Task 3: Project Gantt Data and Summary

**Files:**
- Create: `src/main/java/com/jitong/projectflow/project/domain/ProjectNodeStatus.java`
- Create: `src/main/java/com/jitong/projectflow/project/domain/GanttNodeSummaryCalculator.java`
- Create: `src/main/java/com/jitong/projectflow/project/entity/ProjectNodeEntity.java`
- Create: `src/main/java/com/jitong/projectflow/project/mapper/ProjectNodeMapper.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/GanttNodeResponse.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/GanttSummaryResponse.java`
- Create: `src/main/java/com/jitong/projectflow/project/dto/ProjectNodeUpdateRequest.java`
- Create: `src/main/java/com/jitong/projectflow/project/service/GanttService.java`
- Modify: `src/main/java/com/jitong/projectflow/project/controller/ProjectController.java`
- Test: `src/test/java/com/jitong/projectflow/project/domain/GanttNodeSummaryCalculatorTest.java`

- [x] **Step 1: Write failing calculator tests**

Test completed, overdue, due-soon, and normal nodes.

- [x] **Step 2: Implement calculator**

Rules:

- Completed nodes have actual end date or status `COMPLETED`.
- Overdue nodes have no actual end date and today is after planned end date.
- Due-soon nodes have no actual end date and planned end date is within 7 days.
- Overall progress is average `progress_percent` rounded down.

- [x] **Step 3: Implement APIs**

```text
GET    /api/projects/{projectId}/gantt
PATCH  /api/projects/{projectId}/nodes/{nodeId}
GET    /api/projects/{projectId}/gantt/summary
```

Update operations must check node belongs to project and write operation logs.

- [x] **Step 4: Verify**

Run:

```bash
mvn -q -Dtest=GanttNodeSummaryCalculatorTest test
```

Expected: tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/project src/test/java/com/jitong/projectflow/project
git commit -m "feat: add project gantt workflow"
```

## Task 4: Notification Center

**Files:**
- Create: `src/main/java/com/jitong/projectflow/notice/domain/NoticeType.java`
- Create: `src/main/java/com/jitong/projectflow/notice/entity/NoticeEntity.java`
- Create: `src/main/java/com/jitong/projectflow/notice/mapper/NoticeMapper.java`
- Create: `src/main/java/com/jitong/projectflow/notice/dto/NoticeResponse.java`
- Create: `src/main/java/com/jitong/projectflow/notice/service/NoticeService.java`
- Create: `src/main/java/com/jitong/projectflow/notice/controller/NoticeController.java`
- Test: `src/test/java/com/jitong/projectflow/notice/service/NoticeServiceTest.java`

- [x] **Step 1: Implement notice types**

```java
package com.jitong.projectflow.notice.domain;

public enum NoticeType {
    TASK_ASSIGNED,
    BUG_ASSIGNED,
    BUG_COMMENT,
    PROJECT_WARNING,
    REQUIREMENT_STATUS_CHANGED,
    SYSTEM
}
```

- [x] **Step 2: Implement service contract**

Service methods:

```java
void create(Long receiverId, NoticeType type, String title, String content, String businessType, Long businessId);
long unreadCount(Long receiverId);
void markRead(Long receiverId, Long noticeId);
void markAllRead(Long receiverId);
```

- [x] **Step 3: Implement APIs**

```text
GET    /api/notices
GET    /api/notices/unread-count
PATCH  /api/notices/{id}/read
PATCH  /api/notices/read-all
```

Notice ownership checks must throw `BusinessException(ErrorCode.NOT_FOUND, "Notice not found")`.

- [x] **Step 4: Verify**

Run:

```bash
mvn -q -Dtest=NoticeServiceTest test
```

Expected: tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/notice src/test/java/com/jitong/projectflow/notice
git commit -m "feat: add notification center"
```

## Task 5: Excel Export Foundation

**Files:**
- Create: `src/main/java/com/jitong/projectflow/export/controller/ExportController.java`
- Create: `src/main/java/com/jitong/projectflow/export/service/ExcelExportService.java`
- Create: `src/main/java/com/jitong/projectflow/export/dto/OperationLogExportRow.java`
- Create: `src/main/java/com/jitong/projectflow/export/dto/TaskExportRow.java`
- Create: `src/main/java/com/jitong/projectflow/export/dto/RequirementExportRow.java`
- Create: `src/main/java/com/jitong/projectflow/export/dto/BugExportRow.java`
- Create: `src/main/java/com/jitong/projectflow/export/dto/GanttExportRow.java`
- Test: `src/test/java/com/jitong/projectflow/export/service/ExcelExportServiceTest.java`

- [x] **Step 1: Implement export row DTOs**

Each DTO should use EasyExcel `@ExcelProperty` annotations with stable column names.

- [x] **Step 2: Implement export service**

Service accepts row lists and writes XLSX bytes to an output stream.

- [x] **Step 3: Implement endpoints**

```text
GET    /api/exports/operation-logs
GET    /api/exports/tasks
GET    /api/exports/requirements
GET    /api/exports/bugs
GET    /api/exports/projects/{projectId}/gantt
```

Each endpoint sets:

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="<name>.xlsx"
```

- [x] **Step 4: Verify**

Run:

```bash
mvn -q -Dtest=ExcelExportServiceTest test
```

Expected: generated XLSX bytes are non-empty.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/export src/test/java/com/jitong/projectflow/export
git commit -m "feat: add excel export foundation"
```

## Task 6: Personal Statistics and Dashboard Enhancement

**Files:**
- Create: `src/main/java/com/jitong/projectflow/dashboard/dto/MyStatisticsResponse.java`
- Create: `src/main/java/com/jitong/projectflow/dashboard/service/DashboardService.java`
- Modify: `src/main/java/com/jitong/projectflow/dashboard/controller/DashboardController.java`
- Test: `src/test/java/com/jitong/projectflow/dashboard/service/DashboardServiceTest.java`

- [x] **Step 1: Define statistics response**

Fields:

```text
myTaskTotal
myTaskCompleted
myTaskOverdue
myBugTotal
myBugOpen
myRequirementTotal
myRequirementAccepted
unreadNoticeCount
```

- [x] **Step 2: Implement dashboard service**

Service aggregates current-user data from task, bug, requirement, and notice mappers or services.

- [x] **Step 3: Add API**

```text
GET /api/dashboard/my-statistics
```

- [x] **Step 4: Verify**

Run:

```bash
mvn -q -Dtest=DashboardServiceTest test
```

Expected: aggregation tests pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/dashboard src/test/java/com/jitong/projectflow/dashboard
git commit -m "feat: enhance dashboard statistics"
```

## Task 7: Phase 2 Verification

**Files:**
- Modify only files needed to fix concrete compile or test failures.

- [x] **Step 1: Run full tests**

```bash
mvn test
```

Expected: all tests pass.

- [x] **Step 2: Compile package**

```bash
mvn package
```

Expected: package succeeds.

- [x] **Step 3: Run application**

```bash
mvn spring-boot:run
```

Expected: application starts and Swagger exposes Phase 2 endpoints.

- [x] **Step 4: Commit verification fixes**

If fixes were needed:

```bash
git add pom.xml src/main/java src/main/resources src/test/java
git commit -m "fix: stabilize phase 2 backend workflows"
```

Do not create an empty commit.

## Definition of Done

- Requirement workflow APIs exist and enforce status transitions.
- Requirement operations write operation logs.
- Gantt APIs expose node list, summary, and node update behavior.
- Notification inbox supports list, unread count, mark read, and mark all read.
- Excel export endpoints exist for logs, tasks, requirements, BUGs, and Gantt data.
- Dashboard exposes current-user statistics.
- Phase 2 migration is additive and non-destructive.
- Tests cover the main business rules.

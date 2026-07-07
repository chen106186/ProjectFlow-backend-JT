# ProjectFlow Backend Phase 10 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement backend task-calendar month and day APIs.

**Architecture:** Add a focused `taskcalendar` module that reads `pf_task` through `TaskMapper`, maps tasks into calendar DTOs, and centralizes date matching plus sorting. No schema change is needed.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito.

---

### Task 1: Calendar Sorting And Date Matching

**Files:**
- Create: `src/test/java/com/jitong/projectflow/taskcalendar/service/TaskCalendarServiceTest.java`
- Create: `src/main/java/com/jitong/projectflow/taskcalendar/service/TaskCalendarService.java`

- [ ] Write failing tests for date coverage, month preview limiting, and task ordering.
- [ ] Implement date range query, in-memory day matching, selected-date status calculation, and sorting.

### Task 2: Calendar API Contract

**Files:**
- Create: `src/main/java/com/jitong/projectflow/taskcalendar/controller/TaskCalendarController.java`
- Create DTOs under: `src/main/java/com/jitong/projectflow/taskcalendar/dto`

- [ ] Expose month and day endpoints.
- [ ] Parse `YYYY-MM` and `YYYY-MM-DD` query parameters.
- [ ] Return `ApiResponse` wrappers consistent with the rest of the backend.

### Task 3: Verification

**Commands:**
- `$env:JAVA_HOME='D:\devTool\jdk21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; mvn "-Dmaven.repo.local=.m2/repository" test`
- `$env:JAVA_HOME='D:\devTool\jdk21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; mvn "-Dmaven.repo.local=.m2/repository" package`

- [ ] Run the targeted task-calendar tests.
- [ ] Run full tests and package build.

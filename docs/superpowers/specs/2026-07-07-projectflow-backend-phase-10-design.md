# ProjectFlow Backend Phase 10 Design

## Overview

Phase 10 adds backend task-calendar APIs from the Word requirements. The frontend is out of scope. The backend returns deterministic month and day task data that can power a calendar grid, daily task panel, and future overdue notification rules.

## Scope

- Add a `taskcalendar` module.
- Add `GET /api/task-calendar/month?month=YYYY-MM`.
- Add `GET /api/task-calendar/day?date=YYYY-MM-DD`.
- Recalculate task calendar status for the selected date using existing task date rules.
- Sort tasks by urgent priority, overdue days, due-soon state, remaining days, priority, and task id.
- Limit month-day preview tasks to the first five while exposing the day's total count.

## Out of Scope

- Frontend calendar rendering.
- New database tables.
- Scheduled notifications.
- Project-level or department-level data authorization.

## Date Matching

A task appears on a calendar date when its planned period covers that date. If `plannedStartDate` is absent, the task appears only on `plannedEndDate`.

## API Contracts

`GET /api/task-calendar/month?month=2026-07` returns one record per day in the month.

`GET /api/task-calendar/day?date=2026-07-07` returns the full sorted task list for that date.

Both endpoints are authenticated-only reads.

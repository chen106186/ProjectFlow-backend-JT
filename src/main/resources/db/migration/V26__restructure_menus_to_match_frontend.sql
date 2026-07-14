-- V26__restructure_menus_to_match_frontend.sql
-- Restructures sys_menu to mirror the frontend sidebar navigation exactly.
--
-- Changes:
--   + Adds GROUP parents: 首页(home), 个人工作(personal), 项目清单(projects), 任务列表(tasks)
--   + Adds personal sub-menus with dedicated codes (personal:statistics / personal:tasks / ...)
--   + Moves existing 'project' and 'task' menus under their new GROUP parents
--   + Re-sorts all top-level and system sub-menus
--   - Removes obsolete system:department and system:menu entries (pages no longer exist in frontend)
--   + Grants all new menus to ADMIN role

-- ─── 1. Clean up obsolete menus (department & menu-management pages removed from frontend) ──
DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT id FROM sys_menu
    WHERE code IN (
        'system:department:view', 'system:department:create', 'system:department:update', 'system:department',
        'system:menu:view',       'system:menu:create',       'system:menu:update',       'system:menu'
    )
);
DELETE FROM sys_menu WHERE code IN (
    'system:department:view', 'system:department:create', 'system:department:update', 'system:department',
    'system:menu:view',       'system:menu:create',       'system:menu:update',       'system:menu'
);

-- ─── 2. Insert top-level anchor nodes ─────────────────────────────────────────────────────────
-- 首页 – public, no permission gate
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001001, NULL, 'home', '首页', 'MENU', '/', 10, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'home');

-- 个人工作 – GROUP (visual parent only)
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001002, NULL, 'personal', '个人工作', 'GROUP', NULL, 20, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'personal');

-- 项目清单 – GROUP (visual parent only)
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001003, NULL, 'projects', '项目清单', 'GROUP', NULL, 30, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'projects');

-- 任务列表 – GROUP (visual parent only)
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001004, NULL, 'tasks', '任务列表', 'GROUP', NULL, 40, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'tasks');

-- ─── 3. Insert personal sub-menu entries ──────────────────────────────────────────────────────
-- These codes allow fine-grained role control over the 个人工作 section.
-- Frontend accessCodes checks both these new codes AND the functional module codes (backward compat).

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001011, 1000000000000001002,
       'personal:statistics', '我的统计', 'MENU', '/personal/statistics', 21, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'personal:statistics');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001012, 1000000000000001002,
       'personal:tasks', '我的任务', 'MENU', '/personal/tasks', 22, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'personal:tasks');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001013, 1000000000000001002,
       'personal:requirements', '我的需求', 'MENU', '/personal/requirements', 23, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'personal:requirements');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001014, 1000000000000001002,
       'personal:bugs', '我的 Bug', 'MENU', '/personal/bugs', 24, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'personal:bugs');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001015, 1000000000000001002,
       'personal:daily', '我的日报', 'MENU', '/personal/daily', 25, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'personal:daily');

-- ─── 4. Re-parent existing functional menus under new GROUPs ──────────────────────────────────
-- 'project' → under 项目清单 GROUP
UPDATE sys_menu SET parent_id = 1000000000000001003, sort_order = 31 WHERE code = 'project';

-- 'task' → under 任务列表 GROUP
UPDATE sys_menu SET parent_id = 1000000000000001004, sort_order = 41 WHERE code = 'task';

-- ─── 5. Re-sort remaining top-level and system sub-menus ──────────────────────────────────────
UPDATE sys_menu SET sort_order = 50  WHERE code = 'requirement';
UPDATE sys_menu SET sort_order = 60  WHERE code = 'bug';
UPDATE sys_menu SET sort_order = 70  WHERE code = 'file';
UPDATE sys_menu SET sort_order = 80  WHERE code = 'daily-report';
UPDATE sys_menu SET sort_order = 90  WHERE code = 'project-report';
UPDATE sys_menu SET sort_order = 100 WHERE code = 'system';
UPDATE sys_menu SET sort_order = 101 WHERE code = 'system:user';
UPDATE sys_menu SET sort_order = 102 WHERE code = 'system:role';
UPDATE sys_menu SET sort_order = 103 WHERE code = 'system:log';

-- ─── 6. Grant all menus (new and existing) to ADMIN role ──────────────────────────────────────
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE m.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
  );

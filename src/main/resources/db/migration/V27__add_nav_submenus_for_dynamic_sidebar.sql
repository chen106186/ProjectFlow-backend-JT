-- V27__add_nav_submenus_for_dynamic_sidebar.sql
-- Splits the monolithic 'project' and 'task' MENU entries into proper sidebar sub-items
-- so the frontend can render the sidebar entirely from backend-returned menus.
--
-- Strategy:
--   - 'project' / 'task' → converted to BUTTON (stays as permission code, hidden from sidebar)
--   - New MENU entries added for each sub-page, parented under existing GROUP nodes
--   - Existing role assignments for 'project'/'task' are propagated to the new sub-entries

-- ─── 1. Add project sub-menus under 项目清单 GROUP ────────────────────────────────────────────
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001021, 1000000000000001003,
       'project:management', '管理类项目', 'MENU', '/projects/management', 31, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project:management');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001022, 1000000000000001003,
       'project:execution', '执行类项目', 'MENU', '/projects/execution', 32, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project:execution');

-- ─── 2. Add task sub-menus under 任务列表 GROUP ───────────────────────────────────────────────
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001031, 1000000000000001004,
       'task:all', '全部任务', 'MENU', '/tasks/all', 41, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'task:all');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001032, 1000000000000001004,
       'task:development', '开发任务', 'MENU', '/tasks/development', 42, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'task:development');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000001033, 1000000000000001004,
       'task:testing', '测试任务', 'MENU', '/tasks/testing', 43, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'task:testing');

-- ─── 3. Demote 'project' and 'task' to BUTTON ────────────────────────────────────────────────
-- They remain as permission codes for @PreAuthorize but are no longer visible in the sidebar.
UPDATE sys_menu SET type = 'BUTTON' WHERE code IN ('project', 'task');

-- ─── 4. Propagate: any role with 'project' gets the two new sub-menus ─────────────────────────
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT srm.role_id, nm.id
FROM sys_role_menu srm
JOIN sys_menu om ON srm.menu_id = om.id AND om.code = 'project'
JOIN sys_menu nm  ON nm.code IN ('project:management', 'project:execution') AND nm.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm2
    WHERE rm2.role_id = srm.role_id AND rm2.menu_id = nm.id
);

-- ─── 5. Propagate: any role with 'task' gets the three new sub-menus ──────────────────────────
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT srm.role_id, nm.id
FROM sys_role_menu srm
JOIN sys_menu om ON srm.menu_id = om.id AND om.code = 'task'
JOIN sys_menu nm  ON nm.code IN ('task:all', 'task:development', 'task:testing') AND nm.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm2
    WHERE rm2.role_id = srm.role_id AND rm2.menu_id = nm.id
);

-- ─── 6. Grant all menus to ADMIN role ────────────────────────────────────────────────────────
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE m.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
  );

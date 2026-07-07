INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000401, NULL, 'project', 'Project Management', 'MENU', '/projects', 100, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000402, NULL, 'task', 'Task Management', 'MENU', '/tasks', 110, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'task');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000403, NULL, 'requirement', 'Requirement Management', 'MENU', '/requirements', 120, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'requirement');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000404, NULL, 'bug', 'Bug Management', 'MENU', '/bugs', 130, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'bug');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000501, (SELECT id FROM sys_menu WHERE code = 'system:log'), 'system:log:view', 'View Operation Logs', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:log:view');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000502, (SELECT id FROM sys_menu WHERE code = 'system:user'), 'system:user:view', 'View Users', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:user:view');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000503, (SELECT id FROM sys_menu WHERE code = 'system:user'), 'system:user:create', 'Create Users', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:user:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000504, (SELECT id FROM sys_menu WHERE code = 'system:user'), 'system:user:update', 'Update Users', 'BUTTON', NULL, 3, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:user:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000505, (SELECT id FROM sys_menu WHERE code = 'system:department'), 'system:department:view', 'View Departments', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:department:view');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000506, (SELECT id FROM sys_menu WHERE code = 'system:department'), 'system:department:create', 'Create Departments', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:department:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000507, (SELECT id FROM sys_menu WHERE code = 'system:department'), 'system:department:update', 'Update Departments', 'BUTTON', NULL, 3, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:department:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000508, (SELECT id FROM sys_menu WHERE code = 'system:role'), 'system:role:view', 'View Roles', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:role:view');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000509, (SELECT id FROM sys_menu WHERE code = 'system:role'), 'system:role:create', 'Create Roles', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:role:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000510, (SELECT id FROM sys_menu WHERE code = 'system:role'), 'system:role:update', 'Update Roles', 'BUTTON', NULL, 3, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:role:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000511, (SELECT id FROM sys_menu WHERE code = 'system:role'), 'system:role:assign-menu', 'Assign Role Menus', 'BUTTON', NULL, 4, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:role:assign-menu');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000512, (SELECT id FROM sys_menu WHERE code = 'system:menu'), 'system:menu:view', 'View Menus', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:menu:view');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000513, (SELECT id FROM sys_menu WHERE code = 'system:menu'), 'system:menu:create', 'Create Menus', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:menu:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000514, (SELECT id FROM sys_menu WHERE code = 'system:menu'), 'system:menu:update', 'Update Menus', 'BUTTON', NULL, 3, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:menu:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000601, (SELECT id FROM sys_menu WHERE code = 'project'), 'project:create', 'Create Projects', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000602, (SELECT id FROM sys_menu WHERE code = 'project'), 'project:update', 'Update Projects', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'project:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000603, (SELECT id FROM sys_menu WHERE code = 'task'), 'task:create', 'Create Tasks', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'task:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000604, (SELECT id FROM sys_menu WHERE code = 'task'), 'task:update', 'Update Tasks', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'task:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000605, (SELECT id FROM sys_menu WHERE code = 'requirement'), 'requirement:create', 'Create Requirements', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'requirement:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000606, (SELECT id FROM sys_menu WHERE code = 'requirement'), 'requirement:update', 'Update Requirements', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'requirement:update');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000607, (SELECT id FROM sys_menu WHERE code = 'bug'), 'bug:create', 'Create Bugs', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'bug:create');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000608, (SELECT id FROM sys_menu WHERE code = 'bug'), 'bug:update', 'Update Bugs', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'bug:update');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE m.code IN (
    'project',
    'task',
    'requirement',
    'bug',
    'system:log:view',
    'system:user:view',
    'system:user:create',
    'system:user:update',
    'system:department:view',
    'system:department:create',
    'system:department:update',
    'system:role:view',
    'system:role:create',
    'system:role:update',
    'system:role:assign-menu',
    'system:menu:view',
    'system:menu:create',
    'system:menu:update',
    'project:create',
    'project:update',
    'task:create',
    'task:update',
    'requirement:create',
    'requirement:update',
    'bug:create',
    'bug:update'
)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
  );

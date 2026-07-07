INSERT INTO sys_department (id, parent_id, name, sort_order, created_by)
SELECT 1000000000000000001, NULL, '综合部', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_department WHERE id = 1000000000000000001);

INSERT INTO sys_role (id, code, name, created_by)
SELECT 1000000000000000101, 'ADMIN', '超级管理员', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 1000000000000000101);

INSERT INTO sys_user (id, department_id, username, password_hash, real_name, enabled, created_by)
SELECT 1000000000000000201, 1000000000000000001, 'admin', '$2a$10$6RSMRuuKFjf8Liin5FnT5Ov/DTaU8KgLgeq5DnEsBRxPEb7cgN/Yq', '系统管理员', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000301, NULL, 'system', '系统管理', 'MENU', '/system', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000302, 1000000000000000301, 'system:user', '用户管理', 'MENU', '/system/users', 10, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:user');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000303, 1000000000000000301, 'system:role', '角色管理', 'MENU', '/system/roles', 20, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:role');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000304, 1000000000000000301, 'system:department', '部门管理', 'MENU', '/system/departments', 30, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:department');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000305, 1000000000000000301, 'system:menu', '菜单管理', 'MENU', '/system/menus', 40, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:menu');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000306, 1000000000000000301, 'system:log', '操作日志', 'MENU', '/system/logs', 50, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'system:log');

INSERT INTO sys_user_role (user_id, role_id)
SELECT 1000000000000000201, 1000000000000000101
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id = 1000000000000000201 AND role_id = 1000000000000000101
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE m.code IN ('system', 'system:user', 'system:role', 'system:department', 'system:menu', 'system:log')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
  );

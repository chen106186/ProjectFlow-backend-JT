-- 新增"查看全部业务数据"权限节点
INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000700, NULL, 'data:view:all', '查看全部业务数据', 'BUTTON', NULL, 999, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'data:view:all');

-- 新增"部门领导"角色
INSERT INTO sys_role (id, code, name, created_by)
SELECT 1000000000000000102, 'LEADER', '部门领导', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE id = 1000000000000000102);

-- 为"部门领导"角色分配权限：查看全部业务数据 + 常规业务操作权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000102, m.id
FROM sys_menu m
WHERE m.code IN (
    'data:view:all',
    'project',
    'task',
    'requirement',
    'bug',
    'project:create',
    'project:update',
    'task:create',
    'task:update',
    'requirement:create',
    'requirement:update',
    'bug:create',
    'bug:update',
    'system:log:view'
)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1000000000000000102 AND rm.menu_id = m.id
  );

-- 将系统设置组的名称和路径对齐前端 /settings/*
UPDATE sys_menu SET name = '系统设置', path = '/settings', sort_order = 200 WHERE code = 'system';
UPDATE sys_menu SET path = '/settings/users'      WHERE code = 'system:user';
UPDATE sys_menu SET path = '/settings/roles'      WHERE code = 'system:role';
UPDATE sys_menu SET path = '/settings/departments' WHERE code = 'system:department';
UPDATE sys_menu SET path = '/settings/menus'      WHERE code = 'system:menu';
UPDATE sys_menu SET path = '/settings/logs'       WHERE code = 'system:log';

-- 将主功能菜单名称对齐前端导航标签
UPDATE sys_menu SET name = '项目清单',    path = '/projects',       sort_order = 100 WHERE code = 'project';
UPDATE sys_menu SET name = '任务管理',    path = '/tasks',          sort_order = 110 WHERE code = 'task';
UPDATE sys_menu SET name = '需求管理',    path = '/requirements',   sort_order = 120 WHERE code = 'requirement';
UPDATE sys_menu SET name = 'Bug 列表',   path = '/bugs',           sort_order = 130 WHERE code = 'bug';
UPDATE sys_menu SET name = '文件管理',    path = '/files',          sort_order = 140 WHERE code = 'file';
UPDATE sys_menu SET name = '日报管理',    path = '/daily-reports',  sort_order = 150 WHERE code = 'daily-report';
UPDATE sys_menu SET name = '项目汇报管理', path = '/project-reports', sort_order = 160 WHERE code = 'project-report';

-- 按钮权限名称也一并中文化
UPDATE sys_menu SET name = '查看日志'   WHERE code = 'system:log:view';
UPDATE sys_menu SET name = '查看用户'   WHERE code = 'system:user:view';
UPDATE sys_menu SET name = '创建用户'   WHERE code = 'system:user:create';
UPDATE sys_menu SET name = '编辑用户'   WHERE code = 'system:user:update';
UPDATE sys_menu SET name = '查看部门'   WHERE code = 'system:department:view';
UPDATE sys_menu SET name = '创建部门'   WHERE code = 'system:department:create';
UPDATE sys_menu SET name = '编辑部门'   WHERE code = 'system:department:update';
UPDATE sys_menu SET name = '查看角色'   WHERE code = 'system:role:view';
UPDATE sys_menu SET name = '创建角色'   WHERE code = 'system:role:create';
UPDATE sys_menu SET name = '编辑角色'   WHERE code = 'system:role:update';
UPDATE sys_menu SET name = '分配菜单'   WHERE code = 'system:role:assign-menu';
UPDATE sys_menu SET name = '查看菜单'   WHERE code = 'system:menu:view';
UPDATE sys_menu SET name = '创建菜单'   WHERE code = 'system:menu:create';
UPDATE sys_menu SET name = '编辑菜单'   WHERE code = 'system:menu:update';
UPDATE sys_menu SET name = '创建项目'   WHERE code = 'project:create';
UPDATE sys_menu SET name = '编辑项目'   WHERE code = 'project:update';
UPDATE sys_menu SET name = '创建任务'   WHERE code = 'task:create';
UPDATE sys_menu SET name = '编辑任务'   WHERE code = 'task:update';
UPDATE sys_menu SET name = '创建需求'   WHERE code = 'requirement:create';
UPDATE sys_menu SET name = '编辑需求'   WHERE code = 'requirement:update';
UPDATE sys_menu SET name = '创建 Bug'  WHERE code = 'bug:create';
UPDATE sys_menu SET name = '编辑 Bug'  WHERE code = 'bug:update';
UPDATE sys_menu SET name = '上传文件'   WHERE code = 'file:upload';
UPDATE sys_menu SET name = '删除文件'   WHERE code = 'file:delete';
UPDATE sys_menu SET name = '创建日报'   WHERE code = 'daily-report:create';
UPDATE sys_menu SET name = '编辑日报'   WHERE code = 'daily-report:update';
UPDATE sys_menu SET name = '创建项目汇报' WHERE code = 'project-report:create';
UPDATE sys_menu SET name = '编辑项目汇报' WHERE code = 'project-report:update';

-- 确保超级管理员拥有所有菜单权限（含新增的菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
);

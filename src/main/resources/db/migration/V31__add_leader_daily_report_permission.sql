-- 为"部门领导"角色添加查看日报管理页面的权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000102, m.id
FROM sys_menu m
WHERE m.code = 'daily-report'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1000000000000000102 AND rm.menu_id = m.id
  );

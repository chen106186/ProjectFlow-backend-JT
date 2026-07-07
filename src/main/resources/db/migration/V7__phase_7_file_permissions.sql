INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000701, NULL, 'file', 'File Management', 'MENU', '/files', 140, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'file');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000702, (SELECT id FROM sys_menu WHERE code = 'file'), 'file:upload', 'Upload Files', 'BUTTON', NULL, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'file:upload');

INSERT INTO sys_menu (id, parent_id, code, name, type, path, sort_order, created_by)
SELECT 1000000000000000703, (SELECT id FROM sys_menu WHERE code = 'file'), 'file:delete', 'Delete Files', 'BUTTON', NULL, 2, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'file:delete');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1000000000000000101, m.id
FROM sys_menu m
WHERE m.code IN ('file', 'file:upload', 'file:delete')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1000000000000000101 AND rm.menu_id = m.id
  );

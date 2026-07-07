CREATE INDEX idx_sys_user_role_role ON sys_user_role (role_id);
CREATE INDEX idx_sys_role_menu_menu ON sys_role_menu (menu_id);
CREATE INDEX idx_sys_menu_parent_sort ON sys_menu (parent_id, sort_order);
CREATE INDEX idx_sys_department_parent_sort ON sys_department (parent_id, sort_order);
CREATE INDEX idx_sys_operation_log_module_type_time ON sys_operation_log (module, operation_type, created_at);

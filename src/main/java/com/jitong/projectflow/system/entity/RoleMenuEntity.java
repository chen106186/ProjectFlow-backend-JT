package com.jitong.projectflow.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_menu")
public class RoleMenuEntity {
    private Long roleId;
    private Long menuId;
}

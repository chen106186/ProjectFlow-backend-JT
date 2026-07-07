package com.jitong.projectflow.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class UserRoleEntity {
    private Long userId;
    private Long roleId;
}

package com.jitong.projectflow.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user")
public class SystemUser {
    private Long id;
    private Long departmentId;
    private String username;
    private String passwordHash;
    private String realName;
    private Boolean enabled;
    private Boolean deleted;
}

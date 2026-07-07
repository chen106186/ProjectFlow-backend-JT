package com.jitong.projectflow.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SystemUser {
    private Long id;
    private Long departmentId;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String email;
    private String jobNo;
    private String positionName;
    private Boolean enabled;
    private LocalDate hireDate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}

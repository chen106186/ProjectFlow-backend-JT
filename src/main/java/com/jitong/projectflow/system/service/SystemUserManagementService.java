package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.UserCreateRequest;
import com.jitong.projectflow.system.dto.UserDetailResponse;
import com.jitong.projectflow.system.dto.UserEnabledUpdateRequest;
import com.jitong.projectflow.system.dto.UserPasswordResetRequest;
import com.jitong.projectflow.system.dto.UserRoleAssignRequest;
import com.jitong.projectflow.system.dto.UserUpdateRequest;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemUserManagementService {
    private final SystemUserMapper systemUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public UserDetailResponse create(UserCreateRequest request) {
        ensureUsernameUnique(request.getUsername(), null);
        SystemUser user = new SystemUser();
        user.setDepartmentId(request.getDepartmentId());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setJobNo(request.getJobNo());
        user.setPositionName(request.getPositionName());
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        user.setHireDate(request.getHireDate());
        user.setCreatedBy(CurrentUserContext.userIdOrNull());
        user.setCreatedAt(LocalDateTime.now());
        systemUserMapper.insert(user);
        operationLogService.record("system", "User", user.getId(), "CREATE", "Create user " + user.getUsername());
        return toDetailResponse(user, List.of());
    }

    public UserDetailResponse getById(Long id) {
        SystemUser user = requireUser(id);
        return toDetailResponse(user, roleIds(id));
    }

    public UserDetailResponse update(Long id, UserUpdateRequest request) {
        SystemUser user = requireUser(id);
        if (request.getDepartmentId() != null) user.setDepartmentId(request.getDepartmentId());
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getJobNo() != null) user.setJobNo(request.getJobNo());
        if (request.getPositionName() != null) user.setPositionName(request.getPositionName());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        if (request.getHireDate() != null) user.setHireDate(request.getHireDate());
        user.setUpdatedBy(CurrentUserContext.userIdOrNull());
        user.setUpdatedAt(LocalDateTime.now());
        systemUserMapper.updateById(user);
        operationLogService.record("system", "User", id, "UPDATE", "Update user " + user.getUsername());
        return toDetailResponse(user, roleIds(id));
    }

    public UserDetailResponse updateEnabled(Long id, UserEnabledUpdateRequest request) {
        SystemUser user = requireUser(id);
        user.setEnabled(request.getEnabled());
        user.setUpdatedBy(CurrentUserContext.userIdOrNull());
        user.setUpdatedAt(LocalDateTime.now());
        systemUserMapper.updateById(user);
        operationLogService.record("system", "User", id, "ENABLE_CHANGED",
                (Boolean.TRUE.equals(request.getEnabled()) ? "Enable" : "Disable") + " user " + user.getUsername());
        return toDetailResponse(user, roleIds(id));
    }

    public void resetPassword(Long id, UserPasswordResetRequest request) {
        SystemUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUpdatedBy(CurrentUserContext.userIdOrNull());
        user.setUpdatedAt(LocalDateTime.now());
        systemUserMapper.updateById(user);
        operationLogService.record("system", "User", id, "RESET_PASSWORD", "Reset password for user " + user.getUsername());
    }

    public List<Long> assignRoles(Long id, UserRoleAssignRequest request) {
        requireUser(id);
        List<Long> roleIds = request.getRoleIds() == null ? List.of() : request.getRoleIds();
        userRoleMapper.deleteByUserId(id);
        for (Long roleId : roleIds) {
            userRoleMapper.insertRelation(id, roleId);
        }
        operationLogService.record("system", "User", id, "ASSIGN_ROLES", "Assign roles to user " + id);
        return new ArrayList<>(roleIds);
    }

    private void ensureUsernameUnique(String username, Long currentId) {
        LambdaQueryWrapper<SystemUser> wrapper = new LambdaQueryWrapper<SystemUser>()
                .eq(SystemUser::getUsername, username);
        if (currentId != null) {
            wrapper.ne(SystemUser::getId, currentId);
        }
        if (systemUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
    }

    private SystemUser requireUser(Long id) {
        SystemUser user = systemUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<Long> roleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    private UserDetailResponse toDetailResponse(SystemUser user, List<Long> roleIds) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .departmentId(user.getDepartmentId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .jobNo(user.getJobNo())
                .positionName(user.getPositionName())
                .enabled(user.getEnabled())
                .hireDate(user.getHireDate())
                .roleIds(roleIds)
                .build();
    }
}

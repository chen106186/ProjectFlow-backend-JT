package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import com.jitong.projectflow.system.mapper.MenuMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.RoleMenuMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CurrentUserPermissionService {
    private final SystemUserMapper systemUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final DepartmentMapper departmentMapper;

    public CurrentUserProfileResponse getCurrentUser() {
        Long userId = CurrentUserContext.userId();
        SystemUser user = requireEnabledUser(userId);
        List<RoleEntity> roles = loadUserRoles(userId);
        List<MenuEntity> allMenus = loadMenusForRoles(roles, userId);

        String departmentName = null;
        if (user.getDepartmentId() != null) {
            DepartmentEntity dept = departmentMapper.selectById(user.getDepartmentId());
            if (dept != null) departmentName = dept.getName();
        }

        return CurrentUserProfileResponse.builder()
                .id(user.getId())
                .departmentId(user.getDepartmentId())
                .departmentName(departmentName)
                .username(user.getUsername())
                .realName(user.getRealName())
                .jobNo(user.getJobNo())
                .positionName(user.getPositionName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .hireDate(user.getHireDate())
                .roles(roles.stream().map(this::toRoleResponse).toList())
                .menus(allMenus.stream()
                        .filter(menu -> !"BUTTON".equals(menu.getType()))
                        .map(this::toMenuResponse)
                        .toList())
                .permissions(extractPermissions(allMenus))
                .isGmOffice(CurrentUserContext.isGmOffice())
                .build();
    }

    public List<String> getCurrentUserPermissions() {
        return getCurrentUser().getPermissions();
    }

    public List<String> getPermissionsByUserId(Long userId) {
        List<RoleEntity> roles = loadUserRoles(userId);
        List<MenuEntity> menus = loadMenusForRoles(roles, userId);
        return extractPermissions(menus);
    }

    public String getDeptNameByUserId(Long userId) {
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return null;
        }
        DepartmentEntity dept = departmentMapper.selectById(user.getDepartmentId());
        return dept != null ? dept.getName() : null;
    }

    private SystemUser requireEnabledUser(Long userId) {
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户已被禁用");
        }
        return user;
    }

    private List<RoleEntity> loadUserRoles(Long userId) {
        requireEnabledUser(userId);
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds));
    }

    private List<MenuEntity> loadMenusForRoles(List<RoleEntity> roles, Long userId) {
        requireEnabledUser(userId);
        if (roles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = roles.stream().map(RoleEntity::getId).toList();
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleIds(roleIds).stream().distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }

        return menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                .in(MenuEntity::getId, menuIds)
                .orderByAsc(MenuEntity::getSortOrder)
                .orderByAsc(MenuEntity::getId));
    }

    private List<String> extractPermissions(List<MenuEntity> menus) {
        Set<String> permissions = new LinkedHashSet<>();
        for (MenuEntity menu : menus) {
            if (menu.getCode() != null && !menu.getCode().isBlank()) {
                permissions.add(menu.getCode());
            }
        }
        return List.copyOf(permissions);
    }

    private RoleResponse toRoleResponse(RoleEntity role) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .build();
    }

    private MenuResponse toMenuResponse(MenuEntity menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .code(menu.getCode())
                .name(menu.getName())
                .type(menu.getType())
                .path(menu.getPath())
                .sortOrder(menu.getSortOrder())
                .build();
    }
}

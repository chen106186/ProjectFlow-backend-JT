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
import com.jitong.projectflow.system.entity.RoleMenuEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.entity.UserRoleEntity;
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

    public CurrentUserProfileResponse getCurrentUser() {
        Long userId = CurrentUserContext.userId();
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Current user not found");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user disabled");
        }
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .toList();
        List<RoleEntity> roles = roleIds.isEmpty()
                ? List.of()
                : roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds));
        List<Long> menuIds = roleIds.isEmpty()
                ? List.of()
                : roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuEntity>().in(RoleMenuEntity::getRoleId, roleIds))
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .distinct()
                .toList();
        List<MenuEntity> allMenus = menuIds.isEmpty()
                ? List.of()
                : menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>().in(MenuEntity::getId, menuIds)
                .orderByAsc(MenuEntity::getSortOrder)
                .orderByAsc(MenuEntity::getId));
        Set<String> permissions = new LinkedHashSet<>();
        for (MenuEntity menu : allMenus) {
            permissions.add(menu.getCode());
        }
        return CurrentUserProfileResponse.builder()
                .id(user.getId())
                .departmentId(user.getDepartmentId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roles.stream().map(this::toRoleResponse).toList())
                .menus(allMenus.stream().filter(menu -> "MENU".equals(menu.getType())).map(this::toMenuResponse).toList())
                .permissions(List.copyOf(permissions))
                .build();
    }

    public List<String> getCurrentUserPermissions() {
        return getCurrentUser().getPermissions();
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

package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.SystemUserResponse;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import com.jitong.projectflow.system.mapper.MenuMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemQueryService {
    private final SystemUserMapper systemUserMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    public List<SystemUserResponse> listUsers(String keyword, Long departmentId, Boolean enabled) {
        LambdaQueryWrapper<SystemUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(departmentId != null, SystemUser::getDepartmentId, departmentId);
        wrapper.eq(enabled != null, SystemUser::getEnabled, enabled);
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(SystemUser::getUsername, keyword)
                .or()
                .like(SystemUser::getRealName, keyword));
        wrapper.orderByAsc(SystemUser::getUsername);
        return systemUserMapper.selectList(wrapper).stream().map(this::toUserResponse).toList();
    }

    public List<DepartmentResponse> listDepartments() {
        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DepartmentEntity::getSortOrder).orderByAsc(DepartmentEntity::getId);
        return departmentMapper.selectList(wrapper).stream().map(this::toDepartmentResponse).toList();
    }

    public List<RoleResponse> listRoles() {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RoleEntity::getCode);
        return roleMapper.selectList(wrapper).stream().map(this::toRoleResponse).toList();
    }

    public List<MenuResponse> listMenus() {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MenuEntity::getSortOrder).orderByAsc(MenuEntity::getId);
        return menuMapper.selectList(wrapper).stream().map(this::toMenuResponse).toList();
    }

    private SystemUserResponse toUserResponse(SystemUser user) {
        return SystemUserResponse.builder()
                .id(user.getId())
                .departmentId(user.getDepartmentId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .enabled(user.getEnabled())
                .build();
    }

    private DepartmentResponse toDepartmentResponse(DepartmentEntity department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .parentId(department.getParentId())
                .name(department.getName())
                .sortOrder(department.getSortOrder())
                .build();
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

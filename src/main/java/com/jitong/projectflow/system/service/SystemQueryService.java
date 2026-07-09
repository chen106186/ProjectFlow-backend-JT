package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.SystemUserResponse;
import com.jitong.projectflow.system.dto.SystemUserQueryRequest;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import com.jitong.projectflow.system.mapper.MenuMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemQueryService {
    private final SystemUserMapper systemUserMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;

    public PageResult<SystemUserResponse> listUsers(SystemUserQueryRequest request) {
        LambdaQueryWrapper<SystemUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getDepartmentId() != null, SystemUser::getDepartmentId, request.getDepartmentId());
        wrapper.eq(request.getEnabled() != null, SystemUser::getEnabled, request.getEnabled());
        wrapper.and(StringUtils.hasText(request.getKeyword()), nested -> nested
                .like(SystemUser::getUsername, request.getKeyword())
                .or()
                .like(SystemUser::getRealName, request.getKeyword()));
        if (request.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(request.getRoleId());
            if (userIds.isEmpty()) {
                return PageUtils.toResult(new Page<>(), List.of());
            }
            wrapper.in(SystemUser::getId, userIds);
        }
        wrapper.orderByAsc(SystemUser::getUsername);
        Page<SystemUser> page = systemUserMapper.selectPage(PageUtils.toPage(request), wrapper);

        Map<Long, String> departmentNameMap = departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()
                        .select(DepartmentEntity::getId, DepartmentEntity::getName))
                .stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, DepartmentEntity::getName));

        // load all roles once for name lookup
        Map<Long, String> roleNameMap = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                        .select(RoleEntity::getId, RoleEntity::getName))
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getName));

        return PageUtils.toResult(page, page.getRecords().stream()
                .map(u -> toUserResponse(u, departmentNameMap, roleNameMap))
                .toList());
    }

    public List<DepartmentResponse> listDepartments() {
        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DepartmentEntity::getSortOrder).orderByAsc(DepartmentEntity::getId);
        return departmentMapper.selectList(wrapper).stream().map(this::toDepartmentResponse).toList();
    }

    public List<RoleResponse> listRoles() {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RoleEntity::getSortOrder).orderByAsc(RoleEntity::getCode);
        return roleMapper.selectList(wrapper).stream().map(this::toRoleResponse).toList();
    }

    public List<MenuResponse> listMenus() {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MenuEntity::getSortOrder).orderByAsc(MenuEntity::getId);
        return menuMapper.selectList(wrapper).stream().map(this::toMenuResponse).toList();
    }

    private SystemUserResponse toUserResponse(SystemUser user, Map<Long, String> departmentNameMap, Map<Long, String> roleNameMap) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        List<String> roleNames = roleIds.stream()
                .map(id -> roleNameMap.getOrDefault(id, ""))
                .filter(name -> !name.isEmpty())
                .toList();
        return SystemUserResponse.builder()
                .id(user.getId())
                .departmentId(user.getDepartmentId())
                .departmentName(departmentNameMap.get(user.getDepartmentId()))
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .roleIds(roleIds)
                .roleNames(roleNames)
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
                .description(role.getDescription())
                .enabled(role.getEnabled())
                .sortOrder(role.getSortOrder())
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


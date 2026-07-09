package com.jitong.projectflow.system.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.RoleCreateRequest;
import com.jitong.projectflow.system.dto.RoleDetailResponse;
import com.jitong.projectflow.system.dto.RoleMenuAssignRequest;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.RoleUpdateRequest;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.entity.UserRoleEntity;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.RoleMenuMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleManagementService {
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    private final SystemUserMapper systemUserMapper;
    private final OperationLogService operationLogService;

    public RoleResponse create(RoleCreateRequest request) {
        ensureCodeUnique(request.getCode(), null);
        RoleEntity entity = new RoleEntity();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        roleMapper.insert(entity);
        operationLogService.record("system", "Role", entity.getId(), "CREATE", "Create role " + entity.getCode());
        return toResponse(entity);
    }

    public RoleDetailResponse getById(Long id) {
        RoleEntity entity = requireRole(id);
        return toDetailResponse(entity, menuIds(id));
    }

    public RoleResponse update(Long id, RoleUpdateRequest request) {
        RoleEntity entity = requireRole(id);
        if (request.getCode() != null) {
            ensureCodeUnique(request.getCode(), id);
            entity.setCode(request.getCode());
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getEnabled() != null) entity.setEnabled(request.getEnabled());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        roleMapper.updateById(entity);
        operationLogService.record("system", "Role", id, "UPDATE", "Update role " + entity.getCode());
        return toResponse(entity);
    }

    public void delete(Long id) {
        RoleEntity entity = requireRole(id);
        List<UserRoleEntity> assignments = userRoleMapper.selectByRoleId(id);
        for (UserRoleEntity assignment : assignments) {
            SystemUser user = systemUserMapper.selectById(assignment.getUserId());
            if (user != null && Boolean.TRUE.equals(user.getEnabled())) {
                throw new BusinessException(ErrorCode.CONFLICT, "角色已分配给活跃用户，无法删除");
            }
        }
        roleMapper.deleteById(id);
        operationLogService.record("system", "Role", id, "DELETE", "Delete role " + entity.getCode());
    }

    public List<Long> getMenuIds(Long roleId) {
        requireRole(roleId);
        return menuIds(roleId);
    }

    public List<Long> assignMenus(Long roleId, RoleMenuAssignRequest request) {
        requireRole(roleId);
        List<Long> menuIds = request.getMenuIds() == null ? List.of() : request.getMenuIds();
        roleMenuMapper.deleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            roleMenuMapper.insertRelation(roleId, menuId);
        }
        operationLogService.record("system", "Role", roleId, "ASSIGN_MENUS", "Assign menus to role " + roleId);
        return new ArrayList<>(menuIds);
    }

    private void ensureCodeUnique(String code, Long currentId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RoleEntity> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code);
        if (currentId != null) {
            wrapper.ne(RoleEntity::getId, currentId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
    }

    private RoleEntity requireRole(Long id) {
        RoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return entity;
    }

    private List<Long> menuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    private RoleResponse toResponse(RoleEntity entity) {
        return RoleResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .enabled(entity.getEnabled())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    private RoleDetailResponse toDetailResponse(RoleEntity entity, List<Long> menuIds) {
        return RoleDetailResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .enabled(entity.getEnabled())
                .sortOrder(entity.getSortOrder())
                .menuIds(menuIds)
                .build();
    }
}

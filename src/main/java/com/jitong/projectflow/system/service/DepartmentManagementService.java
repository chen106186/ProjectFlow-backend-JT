package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.DepartmentCreateRequest;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.DepartmentUpdateRequest;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentManagementService {
    private final DepartmentMapper departmentMapper;
    private final SystemUserMapper systemUserMapper;
    private final OperationLogService operationLogService;

    public DepartmentResponse create(DepartmentCreateRequest request) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setParentId(request.getParentId());
        entity.setName(request.getName());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        departmentMapper.insert(entity);
        operationLogService.record("system", "Department", entity.getId(), "CREATE", "新建部门：" + entity.getName());
        return toResponse(entity);
    }

    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        DepartmentEntity entity = requireDepartment(id);
        if (request.getParentId() != null) entity.setParentId(request.getParentId());
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        departmentMapper.updateById(entity);
        operationLogService.record("system", "Department", id, "UPDATE", "编辑部门：" + entity.getName());
        return toResponse(entity);
    }

    public void delete(Long id) {
        DepartmentEntity entity = requireDepartment(id);
        Long userCount = systemUserMapper.selectCount(new LambdaQueryWrapper<SystemUser>()
                .eq(SystemUser::getDepartmentId, id)
                .eq(SystemUser::getEnabled, true)
                .eq(SystemUser::getDeleted, false));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门下存在活跃用户，无法删除");
        }
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门下存在子部门，无法删除");
        }
        departmentMapper.deleteById(id);
        operationLogService.record("system", "Department", id, "DELETE", "删除部门：" + entity.getName());
    }

    private DepartmentEntity requireDepartment(Long id) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        return entity;
    }

    private DepartmentResponse toResponse(DepartmentEntity entity) {
        return DepartmentResponse.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}

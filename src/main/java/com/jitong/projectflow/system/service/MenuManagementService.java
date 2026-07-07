package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.MenuCreateRequest;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.MenuUpdateRequest;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuManagementService {
    private static final Set<String> ALLOWED_TYPES = Set.of("MENU", "BUTTON");

    private final MenuMapper menuMapper;
    private final OperationLogService operationLogService;

    public MenuResponse create(MenuCreateRequest request) {
        validateType(request.getType());
        ensureCodeUnique(request.getCode(), null);
        MenuEntity entity = new MenuEntity();
        entity.setParentId(request.getParentId());
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setPath(request.getPath());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        menuMapper.insert(entity);
        operationLogService.record("system", "Menu", entity.getId(), "CREATE", "Create menu " + entity.getCode());
        return toResponse(entity);
    }

    public MenuResponse update(Long id, MenuUpdateRequest request) {
        MenuEntity entity = requireMenu(id);
        if (request.getParentId() != null) entity.setParentId(request.getParentId());
        if (request.getCode() != null) {
            ensureCodeUnique(request.getCode(), id);
            entity.setCode(request.getCode());
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getType() != null) {
            validateType(request.getType());
            entity.setType(request.getType());
        }
        if (request.getPath() != null) entity.setPath(request.getPath());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        menuMapper.updateById(entity);
        operationLogService.record("system", "Menu", id, "UPDATE", "Update menu " + entity.getCode());
        return toResponse(entity);
    }

    public void delete(Long id) {
        MenuEntity entity = requireMenu(id);
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Menu has child menus");
        }
        menuMapper.deleteById(id);
        operationLogService.record("system", "Menu", id, "DELETE", "Delete menu " + entity.getCode());
    }

    private void validateType(String type) {
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid menu type");
        }
    }

    private void ensureCodeUnique(String code, Long currentId) {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getCode, code);
        if (currentId != null) {
            wrapper.ne(MenuEntity::getId, currentId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Menu code already exists");
        }
    }

    private MenuEntity requireMenu(Long id) {
        MenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Menu not found");
        }
        return entity;
    }

    private MenuResponse toResponse(MenuEntity entity) {
        return MenuResponse.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .code(entity.getCode())
                .name(entity.getName())
                .type(entity.getType())
                .path(entity.getPath())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}

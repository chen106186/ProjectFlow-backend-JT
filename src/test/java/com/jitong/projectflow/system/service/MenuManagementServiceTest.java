package com.jitong.projectflow.system.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.MenuCreateRequest;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.mapper.MenuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuManagementServiceTest {
    @Mock
    MenuMapper menuMapper;
    @Mock
    OperationLogService operationLogService;

    @Test
    void createRejectsInvalidType() {
        MenuCreateRequest request = new MenuCreateRequest();
        request.setCode("bad");
        request.setName("Bad");
        request.setType("LINK");

        assertThatThrownBy(() -> new MenuManagementService(menuMapper, operationLogService).create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid menu type");
    }

    @Test
    void createRejectsDuplicateCode() {
        when(menuMapper.selectCount(any())).thenReturn(1L);
        MenuCreateRequest request = new MenuCreateRequest();
        request.setCode("system:user");
        request.setName("User");
        request.setType("MENU");

        assertThatThrownBy(() -> new MenuManagementService(menuMapper, operationLogService).create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Menu code already exists");
    }

    @Test
    void deleteBlockedWhenChildrenExist() {
        MenuEntity menu = new MenuEntity();
        menu.setId(1L);
        menu.setCode("system");
        menu.setName("System");
        when(menuMapper.selectById(1L)).thenReturn(menu);
        when(menuMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> new MenuManagementService(menuMapper, operationLogService).delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Menu has child menus");
    }
}

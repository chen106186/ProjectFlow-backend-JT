package com.jitong.projectflow.system.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.DepartmentCreateRequest;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentManagementServiceTest {
    @Mock
    DepartmentMapper departmentMapper;
    @Mock
    SystemUserMapper systemUserMapper;
    @Mock
    OperationLogService operationLogService;

    @Test
    void createPersistsDepartmentAndWritesLog() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setParentId(1L);
        request.setName("Delivery");
        request.setSortOrder(20);

        new DepartmentManagementService(departmentMapper, systemUserMapper, operationLogService).create(request);

        verify(departmentMapper).insert(any(DepartmentEntity.class));
        verify(operationLogService).record("system", "Department", null, "CREATE", "Create department Delivery");
    }

    @Test
    void deleteBlockedWhenActiveUsersExist() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(1L);
        department.setName("Delivery");
        when(departmentMapper.selectById(1L)).thenReturn(department);
        when(systemUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> new DepartmentManagementService(departmentMapper, systemUserMapper, operationLogService).delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("部门下存在活跃用户，无法删除");
    }

    @Test
    void deleteBlockedWhenChildrenExist() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(1L);
        department.setName("Delivery");
        when(departmentMapper.selectById(1L)).thenReturn(department);
        when(systemUserMapper.selectCount(any())).thenReturn(0L);
        when(departmentMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> new DepartmentManagementService(departmentMapper, systemUserMapper, operationLogService).delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("部门下存在子部门，无法删除");
    }
}

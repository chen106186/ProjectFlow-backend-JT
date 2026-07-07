package com.jitong.projectflow.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.system.dto.SystemUserQueryRequest;
import com.jitong.projectflow.system.entity.DepartmentEntity;
import com.jitong.projectflow.system.entity.MenuEntity;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.DepartmentMapper;
import com.jitong.projectflow.system.mapper.MenuMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemQueryServiceTest {
    @Mock
    SystemUserMapper systemUserMapper;
    @Mock
    DepartmentMapper departmentMapper;
    @Mock
    RoleMapper roleMapper;
    @Mock
    MenuMapper menuMapper;

    @Test
    void listUsersMapsRowsToResponses() {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setDepartmentId(2L);
        user.setUsername("zhangsan");
        user.setRealName("Zhang San");
        user.setEnabled(true);
        Page<SystemUser> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(user));
        when(systemUserMapper.selectPage(any(), any())).thenReturn(page);

        SystemUserQueryRequest request = new SystemUserQueryRequest();
        request.setKeyword("zhang");
        request.setDepartmentId(2L);
        request.setEnabled(true);
        var responses = new SystemQueryService(systemUserMapper, departmentMapper, roleMapper, menuMapper)
                .listUsers(request);

        assertThat(responses.total()).isEqualTo(1);
        assertThat(responses.records().getFirst().getUsername()).isEqualTo("zhangsan");
        assertThat(responses.records().getFirst().getDepartmentId()).isEqualTo(2L);
        assertThat(responses.records().getFirst().getEnabled()).isTrue();
    }

    @Test
    void listDepartmentsRolesAndMenusMapRowsToResponses() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(10L);
        department.setParentId(1L);
        department.setName("R&D");
        department.setSortOrder(10);
        RoleEntity role = new RoleEntity();
        role.setId(20L);
        role.setCode("PM");
        role.setName("Project Manager");
        MenuEntity menu = new MenuEntity();
        menu.setId(30L);
        menu.setParentId(0L);
        menu.setCode("project");
        menu.setName("Project");
        menu.setType("MENU");
        menu.setPath("/projects");
        menu.setSortOrder(1);
        when(departmentMapper.selectList(any())).thenReturn(List.of(department));
        when(roleMapper.selectList(any())).thenReturn(List.of(role));
        when(menuMapper.selectList(any())).thenReturn(List.of(menu));

        SystemQueryService service = new SystemQueryService(systemUserMapper, departmentMapper, roleMapper, menuMapper);

        assertThat(service.listDepartments().getFirst().getName()).isEqualTo("R&D");
        assertThat(service.listRoles().getFirst().getCode()).isEqualTo("PM");
        assertThat(service.listMenus().getFirst().getPath()).isEqualTo("/projects");
    }
}

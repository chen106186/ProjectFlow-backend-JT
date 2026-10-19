package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GanttNodeStatusSchedulerServiceTest {
    @Mock ProjectNodeMapper projectNodeMapper;
    @InjectMocks GanttNodeStatusSchedulerService service;

    @Test
    void concurrentDateOrStatusChangesArePreservedAndNotCountedAsUpdates() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "node-status-test"),
                ProjectNodeEntity.class);
        LocalDate today = LocalDate.of(2026, 10, 19);
        ProjectNodeEntity node = new ProjectNodeEntity();
        node.setId(1L);
        node.setStatus("IN_PROGRESS");
        node.setPlannedStartDate(today.minusDays(5));
        node.setPlannedEndDate(today.minusDays(1));
        node.setActualStartDate(today.minusDays(4));
        when(projectNodeMapper.selectList(any())).thenReturn(List.of(node));
        when(projectNodeMapper.update(isNull(), any(Wrapper.class))).thenAnswer(invocation -> {
            LambdaUpdateWrapper<ProjectNodeEntity> update = invocation.getArgument(1);
            assertThat(update.getSqlSegment()).contains("status", "planned_start_date", "planned_end_date", "actual_start_date", "actual_end_date IS NULL", "updated_at IS NULL");
            assertThat(update.getParamNameValuePairs()).containsValue("IN_PROGRESS");
            assertThat(update.getParamNameValuePairs()).containsValue(today.minusDays(1));
            return 0;
        });

        assertThat(service.syncNodeStatuses0(today)).isZero();
    }
}

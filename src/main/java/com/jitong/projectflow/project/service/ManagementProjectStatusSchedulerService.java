package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagementProjectStatusSchedulerService {

    private final ProjectMapper projectMapper;
    private final ProjectService projectService;

    // 终态不需要重算：COMPLETED、OVERDUE_COMPLETED、PAUSED
    private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "OVERDUE_COMPLETED", "PAUSED", "CANCELLED");

    /** 每天凌晨 1:00 重算所有活跃项目（管理类+执行类）的状态（处理 DUE_SOON / OVERDUE 的时间推移）。 */
    @Scheduled(cron = "${projectflow.schedule.management-project-status-sync-cron:0 0 1 * * ?}")
    public void syncProjectStatuses() {
        int updated = syncProjectStatuses0();
        log.info("[定时] 项目状态同步完成，共更新 {} 条", updated);
    }

    public int syncProjectStatuses0() {
        List<ProjectEntity> projects = projectMapper.selectList(
                new LambdaQueryWrapper<ProjectEntity>()
                        .notIn(ProjectEntity::getStatus, TERMINAL_STATUSES));

        int updated = 0;
        for (ProjectEntity project : projects) {
            String calculated = projectService.calculateProjectStatus(project, null);
            if (!calculated.equals(project.getStatus())) {
                projectMapper.update(null, new LambdaUpdateWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getId, project.getId())
                        .set(ProjectEntity::getStatus, calculated));
                updated++;
            }
        }
        return updated;
    }
}

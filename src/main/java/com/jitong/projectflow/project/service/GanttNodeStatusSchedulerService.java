package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.project.domain.ProjectNodeStatus;
import com.jitong.projectflow.project.domain.ProjectNodeStatusCalculator;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GanttNodeStatusSchedulerService {

    private final ProjectNodeMapper projectNodeMapper;
    private final ProjectNodeStatusCalculator statusCalculator = new ProjectNodeStatusCalculator();

    // 终态不需要重算：已完成、逾期完成
    private static final Set<String> TERMINAL_STATUSES = Set.of(
            ProjectNodeStatus.COMPLETED.name(),
            ProjectNodeStatus.OVERDUE_COMPLETED.name());

    /** 每天凌晨 1:15 重算所有活跃甘特图节点的状态（处理 OVERDUE_START / DUE_SOON / OVERDUE 的时间推移）。 */
    @Scheduled(cron = "${projectflow.schedule.gantt-node-status-sync-cron:0 15 1 * * ?}")
    public void syncNodeStatuses() {
        int updated = syncNodeStatuses0(LocalDate.now());
        log.info("[定时] 甘特图节点状态同步完成，共更新 {} 条", updated);
    }

    public int syncNodeStatuses0(LocalDate today) {
        List<ProjectNodeEntity> nodes = projectNodeMapper.selectList(
                new LambdaQueryWrapper<ProjectNodeEntity>()
                        .notIn(ProjectNodeEntity::getStatus, TERMINAL_STATUSES));

        int updated = 0;
        for (ProjectNodeEntity node : nodes) {
            ProjectNodeStatus calculated = statusCalculator.calculate(
                    node.getPlannedStartDate(), node.getPlannedEndDate(),
                    node.getActualStartDate(), node.getActualEndDate(), today);
            if (!calculated.name().equals(node.getStatus())) {
                projectNodeMapper.update(null, new LambdaUpdateWrapper<ProjectNodeEntity>()
                        .eq(ProjectNodeEntity::getId, node.getId())
                        .set(ProjectNodeEntity::getStatus, calculated.name()));
                updated++;
            }
        }
        return updated;
    }
}

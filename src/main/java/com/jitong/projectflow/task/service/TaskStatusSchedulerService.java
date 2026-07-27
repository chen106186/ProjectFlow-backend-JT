package com.jitong.projectflow.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.domain.TaskStatusCalculator;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusSchedulerService {

    private final TaskMapper taskMapper;
    private final TaskStatusCalculator statusCalculator = new TaskStatusCalculator();

    /** 每天凌晨 0:30 将所有活跃任务的状态同步到数据库 */
    @Scheduled(cron = "${projectflow.schedule.task-status-sync-cron:0 30 0 * * ?}")
    public void syncTaskStatuses() {
        int updated = syncTaskStatuses(LocalDate.now());
        log.info("[定时] 任务状态同步完成，共更新 {} 条", updated);
    }

    public int syncTaskStatuses(LocalDate today) {
        // 跳过 PAUSED（用户手动暂停，不自动变更）；COMPLETED 也需每日重算，以纠正"实际完成时间未到今日"的误标
        List<TaskEntity> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TaskEntity>()
                        .ne(TaskEntity::getStatus, TaskStatus.PAUSED.name()));

        int updated = 0;
        for (TaskEntity task : tasks) {
            TaskStatus calculated = statusCalculator.calculate(
                    task.getPlannedStartDate(),
                    task.getPlannedEndDate(),
                    task.getActualStartDate(),
                    task.getActualEndDate(),
                    false,
                    today);
            if (!calculated.name().equals(task.getStatus())) {
                taskMapper.update(null, new LambdaUpdateWrapper<TaskEntity>()
                        .eq(TaskEntity::getId, task.getId())
                        .set(TaskEntity::getStatus, calculated.name()));
                updated++;
            }
        }
        return updated;
    }
}

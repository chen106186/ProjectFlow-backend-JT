package com.jitong.projectflow.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.dashboard.dto.MyStatisticsResponse;
import com.jitong.projectflow.notice.entity.NoticeEntity;
import com.jitong.projectflow.notice.mapper.NoticeMapper;
import com.jitong.projectflow.requirement.domain.RequirementStatus;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskMapper taskMapper;
    private final BugMapper bugMapper;
    private final RequirementMapper requirementMapper;
    private final NoticeMapper noticeMapper;

    public MyStatisticsResponse getMyStatistics(Long userId) {
        long myTaskTotal = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(TaskEntity::getAssigneeId, userId));

        long myTaskCompleted = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(TaskEntity::getAssigneeId, userId)
                        .eq(TaskEntity::getStatus, TaskStatus.COMPLETED.name()));

        long myTaskOverdue = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(TaskEntity::getAssigneeId, userId)
                        .eq(TaskEntity::getStatus, TaskStatus.OVERDUE.name()));

        long myBugTotal = bugMapper.selectCount(
                new LambdaQueryWrapper<BugEntity>()
                        .eq(BugEntity::getCreatorId, userId));

        long myBugOpen = bugMapper.selectCount(
                new LambdaQueryWrapper<BugEntity>()
                        .eq(BugEntity::getCreatorId, userId)
                        .ne(BugEntity::getStatus, BugStatus.CLOSED.name()));

        long myRequirementTotal = requirementMapper.selectCount(
                new LambdaQueryWrapper<RequirementEntity>()
                        .eq(RequirementEntity::getCreatedBy, userId));

        long myRequirementAccepted = requirementMapper.selectCount(
                new LambdaQueryWrapper<RequirementEntity>()
                        .eq(RequirementEntity::getCreatedBy, userId)
                        .eq(RequirementEntity::getStatus, RequirementStatus.ACCEPTED.name()));

        long unreadNoticeCount = noticeMapper.selectCount(
                new LambdaQueryWrapper<NoticeEntity>()
                        .eq(NoticeEntity::getReceiverId, userId)
                        .eq(NoticeEntity::getReadFlag, 0));

        return MyStatisticsResponse.builder()
                .myTaskTotal(myTaskTotal)
                .myTaskCompleted(myTaskCompleted)
                .myTaskOverdue(myTaskOverdue)
                .myBugTotal(myBugTotal)
                .myBugOpen(myBugOpen)
                .myRequirementTotal(myRequirementTotal)
                .myRequirementAccepted(myRequirementAccepted)
                .unreadNoticeCount(unreadNoticeCount)
                .build();
    }
}

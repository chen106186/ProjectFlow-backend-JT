package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ProjectStageDeadlineNoticeService {
    private static final int REMIND_DAYS = 3;
    private static final String BUSINESS_TYPE = "ProjectNode";

    private final ProjectNodeMapper projectNodeMapper;
    private final ProjectMapper projectMapper;
    private final NoticeService noticeService;
    private final SystemUserMapper systemUserMapper;

    public ProjectStageDeadlineNoticeService(ProjectNodeMapper projectNodeMapper,
                                             ProjectMapper projectMapper,
                                             NoticeService noticeService,
                                             SystemUserMapper systemUserMapper) {
        this.projectNodeMapper = projectNodeMapper;
        this.projectMapper = projectMapper;
        this.noticeService = noticeService;
        this.systemUserMapper = systemUserMapper;
    }

    @Scheduled(cron = "${projectflow.notice.project-stage-deadline-cron:0 15 9 * * ?}")
    public void scanProjectStageDeadlines() {
        scanProjectStageDeadlines(LocalDate.now());
    }

    public int scanProjectStageDeadlines(LocalDate today) {
        LambdaQueryWrapper<ProjectNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(ProjectNodeEntity::getPlannedEndDate)
                .le(ProjectNodeEntity::getPlannedEndDate, today.plusDays(REMIND_DAYS))
                .ne(ProjectNodeEntity::getStatus, "COMPLETED");
        List<ProjectNodeEntity> nodes = projectNodeMapper.selectList(wrapper);
        int created = 0;
        for (ProjectNodeEntity node : nodes) {
            ProjectEntity project = projectMapper.selectById(node.getProjectId());
            Long receiverId = resolveReceiver(project);
            if (receiverId == null) {
                continue;
            }
            NoticePayload payload = buildPayload(project, node, today);
            if (!noticeService.existsBusinessNotice(receiverId, NoticeType.PROJECT_WARNING,
                    payload.title(), BUSINESS_TYPE, node.getId())) {
                noticeService.create(receiverId, NoticeType.PROJECT_WARNING, payload.title(),
                        payload.content(), BUSINESS_TYPE, node.getId());
                created++;
            }
        }
        return created;
    }

    private Long resolveReceiver(ProjectEntity project) {
        if (project == null) {
            return null;
        }
        return project.getManagerId() != null ? project.getManagerId() : project.getCreatedBy();
    }

    private NoticePayload buildPayload(ProjectEntity project, ProjectNodeEntity node, LocalDate today) {
        String projectName = project == null ? "项目" : project.getName();
        String pmName = resolvePmName(project);
        String meta = "项目负责人：" + pmName;
        if (node.getPlannedEndDate().isBefore(today)) {
            long days = ChronoUnit.DAYS.between(node.getPlannedEndDate(), today);
            String title = "项目「" + projectName + "」" + node.getNodeName() + "阶段已逾期 " + days + " 天，请及时处理";
            return new NoticePayload(title, meta);
        }
        String title = "项目「" + projectName + "」" + node.getNodeName() + "阶段即将到期，请及时处理";
        return new NoticePayload(title, meta);
    }

    private String resolvePmName(ProjectEntity project) {
        if (project == null) return "未知";
        Long pmId = project.getManagerId() != null ? project.getManagerId() : project.getCreatedBy();
        if (pmId == null) return "未知";
        SystemUser user = systemUserMapper.selectById(pmId);
        return user != null ? user.getRealName() : "未知";
    }

    private record NoticePayload(String title, String content) {
    }
}

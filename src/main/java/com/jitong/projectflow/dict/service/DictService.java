package com.jitong.projectflow.dict.service;

import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.dict.dto.DictGroupResponse;
import com.jitong.projectflow.dict.dto.DictItemResponse;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.requirement.domain.RequirementPriority;
import com.jitong.projectflow.requirement.domain.RequirementStatus;
import com.jitong.projectflow.task.domain.TaskPriority;
import com.jitong.projectflow.task.domain.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictService {
    private final Map<String, DictGroupResponse> groups = new LinkedHashMap<>();

    public DictService() {
        add("projectType", "项目类型",
                item("MANAGEMENT", "管理类项目"),
                item("EXECUTION", "执行类项目"));
        add("projectBusinessType", "项目业务类型",
                item("DIGITALIZATION", "数字化项目"),
                item("INFORMATIZATION", "信息化项目"),
                item("RESEARCH", "科研项目"),
                item("EXTERNAL", "外部项目"));
        add("projectStage", "项目阶段",
                item("BUSINESS_OPPORTUNITY", "商机跟进"),
                item("FEASIBILITY_APPROVAL", "可研批复"),
                item("BIDDING", "招标"),
                item("CONTRACT_SIGNING", "合同签订"),
                item("PRELIMINARY_APPROVAL", "概设批复"),
                item("REQUIREMENT_ANALYSIS", "需求分析"),
                item("UI_DESIGN", "UI设计"),
                item("DEVELOPMENT", "开发"),
                item("TESTING", "测试"),
                item("TRIAL_RUN", "上线试运行"));
        add("projectStatus", "项目状态",
                item("NOT_STARTED", "未开始"),
                item("IN_PROGRESS", "进行中"),
                item("DUE_SOON", "即将到期"),
                item("OVERDUE", "已逾期"),
                item("PAUSED", "已暂停"),
                item("COMPLETED", "已完成"),
                item("OVERDUE_COMPLETED", "逾期完成"),
                item("CANCELLED", "已取消"));
        add("contractStatus", "合同状态",
                item("NOT_SIGNED", "未签约"),
                item("SIGNING", "签约中"),
                item("SIGNED", "已签约"),
                item("ARCHIVED", "已归档"));
        List<DictItemResponse> taskStatusItems = new ArrayList<>(items(TaskStatus.values(), Map.of(
                TaskStatus.NOT_STARTED.name(), "未开始",
                TaskStatus.IN_PROGRESS.name(), "进行中",
                TaskStatus.DUE_SOON.name(), "即将到期",
                TaskStatus.OVERDUE.name(), "已逾期",
                TaskStatus.COMPLETED.name(), "已完成",
                TaskStatus.PAUSED.name(), "已暂停")));
        // 甘特图节点专用状态（不属于 TaskStatus 枚举，但共用 taskStatus 字典供前端展示）
        taskStatusItems.add(item("OVERDUE_START", "启动逾期"));
        taskStatusItems.add(item("OVERDUE_COMPLETED", "逾期完成"));
        add("taskStatus", "任务状态", taskStatusItems);
        add("taskPriority", "任务优先级", items(TaskPriority.values(), priorityLabels()));
        add("resolveSolution", "Bug解决方案",
                item("RESOLVED",          "已解决"),
                item("BY_DESIGN",         "设计如此"),
                item("DUPLICATE",         "重复bug"),
                item("EXTERNAL_CAUSE",    "外部原因"),
                item("CODE_BUG",          "代码bug"),
                item("CANNOT_REPRODUCE",  "无法重现"),
                item("DEFERRED",          "延期处理"),
                item("WONT_FIX",          "不予解决"));
        add("bugStatus", "Bug 状态", items(BugStatus.values(), Map.of(
                BugStatus.PENDING_FIX.name(), "待修复",
                BugStatus.FIXING.name(), "修复中",
                BugStatus.PENDING_VERIFY.name(), "待验证",
                BugStatus.CLOSED.name(), "已关闭")));
        add("bugPriority", "Bug 优先级", items(TaskPriority.values(), priorityLabels()));
        add("requirementStatus", "需求状态", items(RequirementStatus.values(), Map.of(
                RequirementStatus.PENDING_REVIEW.name(), "待评审",
                RequirementStatus.ACCEPTED.name(), "已采纳",
                RequirementStatus.REJECTED.name(), "已拒绝")));
        add("requirementPriority", "需求优先级", items(RequirementPriority.values(), priorityLabels()));
        add("requirementType", "需求类型",
                item("FUNCTION", "功能需求"),
                item("OPTIMIZATION", "优化需求"),
                item("INTEGRATION", "集成需求"),
                item("OTHER", "其他"));
        add("reportType", "项目汇报类型",
                item("WEEKLY", "周报"),
                item("MONTHLY", "月报"),
                item("MILESTONE", "里程碑汇报"),
                item("MEETING", "会议汇报"),
                item("OTHER", "其他"));
        add("reportStatus", "项目汇报状态",
                item("DRAFT", "草稿"),
                item("PLANNED", "已计划"),
                item("COMPLETED", "已完成"),
                item("CANCELLED", "已取消"));
        add("fileCategory", "文件分类",
                item("CONTRACT", "合同类"),
                item("REQUIREMENT", "需求类"),
                item("DESIGN", "设计类"),
                item("DEVELOPMENT", "开发类"),
                item("ACCEPTANCE", "验收类"));
        add("noticeType", "通知类型", items(NoticeType.values(), Map.of(
                NoticeType.TASK_ASSIGNED.name(), "任务指派",
                NoticeType.BUG_ASSIGNED.name(), "Bug 指派",
                NoticeType.BUG_COMMENT.name(), "Bug 评论",
                NoticeType.PROJECT_WARNING.name(), "项目预警",
                NoticeType.REQUIREMENT_STATUS_CHANGED.name(), "需求状态变更",
                NoticeType.SYSTEM.name(), "系统通知")));
        add("menuType", "菜单类型",
                item("CATALOG", "目录"),
                item("MENU", "菜单"),
                item("BUTTON", "按钮"));
        add("operationType", "操作类型",
                item("CREATE", "新增"),
                item("UPDATE", "编辑"),
                item("DELETE", "删除"),
                item("ASSIGN", "指派"),
                item("CLOSE", "关闭"),
                item("COMMENT", "评论"),
                item("UPDATE_STATUS", "更新状态"),
                item("EXPORT", "导出"));
    }

    public List<DictGroupResponse> listAll() {
        return new ArrayList<>(groups.values());
    }

    public DictGroupResponse getByType(String type) {
        DictGroupResponse group = groups.get(type);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        return group;
    }

    private void add(String type, String name, DictItemResponse... items) {
        groups.put(type, new DictGroupResponse(type, name, List.of(items)));
    }

    private void add(String type, String name, List<DictItemResponse> items) {
        groups.put(type, new DictGroupResponse(type, name, items));
    }

    private DictItemResponse item(String value, String label) {
        return new DictItemResponse(value, label, 0);
    }

    private List<DictItemResponse> items(Enum<?>[] values, Map<String, String> labels) {
        return Arrays.stream(values)
                .map(value -> new DictItemResponse(value.name(), labels.getOrDefault(value.name(), value.name()), value.ordinal()))
                .toList();
    }

    private Map<String, String> priorityLabels() {
        return Map.of(
                "URGENT", "紧急",
                "HIGH", "高",
                "MEDIUM", "中",
                "LOW", "低");
    }
}

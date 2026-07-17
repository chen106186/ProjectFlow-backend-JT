package com.jitong.projectflow.project.domain;

public enum ProjectNodeStatus {
    NOT_STARTED,       // 未开始：未到计划开始时间或计划时间未填
    OVERDUE_START,     // 启动逾期：已过计划开始时间，但实际开始时间仍为空
    IN_PROGRESS,       // 进行中：已填写实际开始时间
    DUE_SOON,          // 即将到期：距计划结束时间不足7天，尚未完成
    OVERDUE,           // 已逾期：已超过计划结束时间，尚未完成
    COMPLETED,         // 已完成：实际结束时间不晚于计划结束时间
    OVERDUE_COMPLETED  // 逾期完成：实际结束时间晚于计划结束时间
}

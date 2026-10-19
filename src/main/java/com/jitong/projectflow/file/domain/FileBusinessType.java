package com.jitong.projectflow.file.domain;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;

import java.util.List;
import java.util.Locale;

/** Includes historical spellings so existing attachments remain accessible under the same policy. */
public enum FileBusinessType {
    PROJECT(List.of("PROJECT", "Project", "ManagementProject", "ExecutionProject", "management-project", "execution-project")),
    TASK(List.of("TASK", "Task", "task")),
    BUG(List.of("BUG", "Bug", "bug")),
    REQUIREMENT(List.of("REQUIREMENT", "Requirement", "requirement")),
    DAILY_REPORT(List.of("DAILY_REPORT", "daily-report", "DailyReport", "daily_report")),
    PROJECT_REPORT(List.of("PROJECT_REPORT", "project-report", "ProjectReport", "project_report")),
    RICH_TEXT(List.of("RICH_TEXT", "rich-text", "RichText", "rich_text"));

    private final List<String> aliases;

    FileBusinessType(List<String> aliases) { this.aliases = aliases; }

    public List<String> aliases() { return aliases; }

    public static FileBusinessType from(String value) {
        if (value == null || value.isBlank()) throw invalidType();
        String normalized = value.trim().replace("-", "_").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PROJECT", "MANAGEMENTPROJECT", "EXECUTIONPROJECT", "MANAGEMENT_PROJECT", "EXECUTION_PROJECT" -> PROJECT;
            case "TASK" -> TASK;
            case "BUG" -> BUG;
            case "REQUIREMENT" -> REQUIREMENT;
            case "DAILY_REPORT", "DAILYREPORT" -> DAILY_REPORT;
            case "PROJECT_REPORT", "PROJECTREPORT" -> PROJECT_REPORT;
            case "RICH_TEXT", "RICHTEXT" -> RICH_TEXT;
            default -> throw invalidType();
        };
    }

    private static BusinessException invalidType() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "不支持的附件业务类型");
    }
}

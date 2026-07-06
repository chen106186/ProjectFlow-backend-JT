package com.jitong.projectflow.dashboard.dto;

import java.time.LocalDate;

public record TodoItemResponse(
        String itemType,
        Long businessId,
        String title,
        String priority,
        String status,
        String projectName,
        String ownerName,
        LocalDate plannedEndDate,
        long overdueDays
) {
}

package com.jitong.projectflow.workhub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class IterationRequests {
    private IterationRequests() {}

    public record StageUpdate(Long ownerId, LocalDate plannedStart, LocalDate plannedEnd,
                              LocalDate actualStart, LocalDate actualEnd,
                              @NotBlank String status, @Size(max = 4000) String conclusion) {}

    public record Record(@NotBlank String stageCode, @NotBlank String recordType,
                         @NotBlank @Size(max = 160) String title,
                         @NotBlank @Size(max = 4000) String content,
                         @Size(max = 1000) String evidenceUrl) {}

    public record Application(@NotBlank @Size(max = 120) String environment,
                              @NotNull LocalDateTime windowStart, @NotNull LocalDateTime windowEnd,
                              @NotBlank @Size(max = 4000) String changeSummary,
                              @NotBlank @Size(max = 4000) String impactScope,
                              @NotBlank @Size(max = 4000) String rollbackPlan,
                              @NotBlank @Size(max = 4000) String monitoringPlan,
                              @NotBlank @Size(max = 4000) String supportPlan,
                              @NotNull Long approverId) {}

    public record Decision(@NotBlank String decision, @NotBlank @Size(max = 2000) String note) {}
    public record Withdrawal(@NotBlank @Size(max = 2000) String reason) {}
}

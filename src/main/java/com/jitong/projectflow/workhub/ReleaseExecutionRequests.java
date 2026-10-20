package com.jitong.projectflow.workhub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ReleaseExecutionRequests {
    private ReleaseExecutionRequests() {}

    public record Plan(@NotBlank @Size(max = 120) String environment,
                       @NotNull LocalDateTime windowStart, @NotNull LocalDateTime windowEnd,
                       @NotNull Long leadId, @NotNull List<Long> operatorIds,
                       Long verifierId, List<Long> supportIds, LocalDateTime meetingPlannedAt) {}

    public record Meeting(@NotNull LocalDateTime heldAt, @NotNull Long chairId,
                          @NotNull List<Long> attendeeIds,
                          @NotBlank @Size(max = 4000) String agenda,
                          @NotBlank @Size(max = 4000) String decisions,
                          @NotNull String risks, @NotNull String actionItems,
                          @NotBlank String conclusion) {}

    public record Report(@NotNull LocalDateTime actualStart, @NotNull LocalDateTime actualEnd,
                         @NotBlank @Size(max = 500) String packageRef,
                         @NotBlank @Size(max = 4000) String executionSteps,
                         @NotBlank @Size(max = 4000) String smokeResult,
                         @NotBlank @Size(max = 4000) String monitoringResult,
                         @NotNull String incidentNotes, @NotNull Boolean rolledBack,
                         @NotBlank String outcome, @NotNull String residualIssues,
                         @NotNull Long verifierId) {}

    public record Verification(@NotBlank String decision, @NotBlank @Size(max = 2000) String note) {}
}

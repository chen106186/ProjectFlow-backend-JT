package com.jitong.projectflow.workhub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class WorkHubRequests {
    private WorkHubRequests() {}

    public record TaskLinks(@NotNull List<Long> taskIds) {}
    public record Acceptance(@NotBlank String result, @Size(max = 2000) String conclusion) {}
    public record Blocker(@Size(max = 2000) String reason) {}
    public record Comment(@NotBlank @Size(max = 4000) String content, List<Long> mentionedUserIds) {}
    public record Release(
            @NotNull Long projectId,
            @NotBlank @Size(max = 160) String name,
            @NotNull LocalDate targetDate,
            @NotBlank String status,
            @Size(max = 4000) String acceptanceNote,
            List<Long> requirementIds,
            List<Long> taskIds) {}
    public record SavedView(@NotBlank @Size(max = 80) String name,
                            @NotBlank String scope,
                            @NotNull Map<String, String> filters) {}
}

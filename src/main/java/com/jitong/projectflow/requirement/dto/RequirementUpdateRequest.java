package com.jitong.projectflow.requirement.dto;

public class RequirementUpdateRequest {

    private String title;

    private String requirementType;

    private String priority;

    private Long projectId;

    private String description;

    private String tags;

    public String title() { return title; }
    public String requirementType() { return requirementType; }
    public String priority() { return priority; }
    public Long projectId() { return projectId; }
    public String description() { return description; }
    public String tags() { return tags; }

    public void setTitle(String title) { this.title = title; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setDescription(String description) { this.description = description; }
    public void setTags(String tags) { this.tags = tags; }
}

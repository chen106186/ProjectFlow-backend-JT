package com.jitong.projectflow.project.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ProjectNodeTemplate {

    DIGITALIZATION(List.of(
            new NodeDef("FEASIBILITY_APPROVAL", "可研批复", 1),
            new NodeDef("BIDDING", "招标", 2),
            new NodeDef("CONTRACT_SIGNING", "合同签订", 3),
            new NodeDef("PRELIMINARY_APPROVAL", "概设批复", 4),
            new NodeDef("REQUIREMENT_ANALYSIS", "需求分析", 5),
            new NodeDef("UI_DESIGN", "UI设计", 6),
            new NodeDef("DEVELOPMENT", "开发", 7),
            new NodeDef("TESTING", "测试", 8),
            new NodeDef("THIRD_PARTY_TESTING", "第三方测试", 9),
            new NodeDef("DEPLOYMENT", "实施部署", 10),
            new NodeDef("TRIAL_RUN", "上线试运行", 11),
            new NodeDef("ACCEPTANCE", "验收", 12),
            new NodeDef("COMPLETION", "完工", 13)
    )),

    RESEARCH(List.of(
            new NodeDef("FEASIBILITY_APPROVAL", "可研批复", 1),
            new NodeDef("PATENT_APPLICATION", "专利申请", 2),
            new NodeDef("PAPER_ACCEPTANCE_SOFTWARE_COPYRIGHT", "论文录用(软著申请)", 3),
            new NodeDef("THIRD_PARTY_EVALUATION", "三方测评", 4),
            new NodeDef("ACCEPTANCE_PREPARATION", "验收准备", 5),
            new NodeDef("COMPLETION", "完工", 6)
    )),

    EXTERNAL(List.of(
            new NodeDef("BUSINESS_STAGE", "商务阶段", 1),
            new NodeDef("USER_RESEARCH", "用户调研", 2),
            new NodeDef("REQUIREMENT_CONFIRMATION", "需求确认", 3),
            new NodeDef("CONTRACT_SIGNING", "合同签订", 4),
            new NodeDef("DEVELOPMENT", "开发", 5),
            new NodeDef("TESTING", "测试", 6),
            new NodeDef("DEPLOYMENT_IMPLEMENTATION", "部署实施", 7),
            new NodeDef("ACCEPTANCE", "验收", 8),
            new NodeDef("COMPLETION", "完工", 9)
    ));

    private final List<NodeDef> nodes;

    private static final Map<String, String> CODE_LABEL_MAP = Stream.of(values())
            .flatMap(t -> t.nodes.stream())
            .collect(Collectors.toMap(NodeDef::code, NodeDef::label, (a, b) -> a));

    ProjectNodeTemplate(List<NodeDef> nodes) {
        this.nodes = nodes;
    }

    public List<NodeDef> getNodes() {
        return nodes;
    }

    public static Optional<ProjectNodeTemplate> forBusinessType(String businessType) {
        if (businessType == null) return Optional.empty();
        try {
            return Optional.of(valueOf(businessType));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static String resolveLabel(String code) {
        if (code == null) return null;
        return CODE_LABEL_MAP.get(code);
    }

    public record NodeDef(String code, String label, int sortOrder) {}
}

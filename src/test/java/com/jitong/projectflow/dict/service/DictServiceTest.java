package com.jitong.projectflow.dict.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.dict.dto.DictGroupResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DictServiceTest {

    private final DictService dictService = new DictService();

    @Test
    void listsAllFrontendDictGroups() {
        assertThat(dictService.listAll())
                .extracting(DictGroupResponse::type)
                .contains("projectType", "taskStatus", "bugStatus", "requirementStatus", "noticeType", "operationType");
    }

    @Test
    void returnsDictByType() {
        DictGroupResponse group = dictService.getByType("taskPriority");

        assertThat(group.name()).isEqualTo("任务优先级");
        assertThat(group.items())
                .extracting("value")
                .containsExactly("URGENT", "HIGH", "MEDIUM", "LOW");
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> dictService.getByType("unknown"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("字典类型不存在");
    }
}

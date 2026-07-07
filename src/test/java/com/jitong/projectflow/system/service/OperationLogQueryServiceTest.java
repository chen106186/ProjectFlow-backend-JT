package com.jitong.projectflow.system.service;

import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogQueryServiceTest {
    @Mock
    OperationLogMapper operationLogMapper;

    @Test
    void listMapsRowsToResponses() {
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setModule("system");
        log.setBusinessType("User");
        log.setBusinessId(2L);
        log.setOperationType("CREATE");
        log.setOperatorId(3L);
        log.setContent("Create user");
        log.setCreatedAt(LocalDateTime.of(2026, 7, 7, 10, 0));
        when(operationLogMapper.selectList(any())).thenReturn(List.of(log));
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setModule("system");

        var responses = new OperationLogQueryService(operationLogMapper).list(request);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getBusinessType()).isEqualTo("User");
        assertThat(responses.getFirst().getContent()).isEqualTo("Create user");
    }
}

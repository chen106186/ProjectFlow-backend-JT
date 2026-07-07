package com.jitong.projectflow.system.audit;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DefaultOperationLogService implements OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public DefaultOperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    @Async
    public void record(String module, String businessType, Long businessId, String operationType, String content) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setOperationType(operationType);
        log.setContent(content);
        log.setCreatedAt(LocalDateTime.now());
        Long userId = CurrentUserContext.userIdOrNull();
        if (userId != null) {
            log.setOperatorId(userId);
        }
        operationLogMapper.insert(log);
    }
}

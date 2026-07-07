package com.jitong.projectflow.system.audit;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DefaultOperationLogService implements OperationLogService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultOperationLogService.class);

    private final OperationLogMapper operationLogMapper;

    public DefaultOperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void record(String module, String businessType, Long businessId, String operationType, String content) {
        try {
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
        } catch (Exception e) {
            logger.error("Failed to write operation log: module={}, businessType={}, businessId={}, op={}",
                    module, businessType, businessId, operationType, e);
        }
    }
}

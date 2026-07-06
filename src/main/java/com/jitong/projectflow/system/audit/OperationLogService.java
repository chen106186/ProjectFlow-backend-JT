package com.jitong.projectflow.system.audit;

public interface OperationLogService {
    void record(String module, String businessType, Long businessId, String operationType, String content);
}

package com.jitong.projectflow.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class BusinessNumberService {
    private final JdbcTemplate jdbcTemplate;

    public long nextBugNo() {
        return next("BUG");
    }

    public long nextRequirementNo() {
        return next("REQUIREMENT");
    }

    private long next(String type) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("业务编号必须在创建事务中分配");
        }
        Long value = jdbcTemplate.queryForObject(
                "SELECT next_value FROM pf_business_number WHERE business_type = ? FOR UPDATE",
                Long.class, type);
        if (value == null) {
            throw new IllegalStateException("缺少业务编号配置：" + type);
        }
        jdbcTemplate.update("UPDATE pf_business_number SET next_value = ? WHERE business_type = ?", value + 1, type);
        return value;
    }
}

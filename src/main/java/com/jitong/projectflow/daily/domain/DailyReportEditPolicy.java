package com.jitong.projectflow.daily.domain;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;

import java.time.LocalDate;
import java.time.ZoneId;

public final class DailyReportEditPolicy {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private DailyReportEditPolicy() {}

    public static void requireEditable(LocalDate date) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (date == null || (date.isBefore(today.minusDays(1)) || date.isAfter(today))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅今天和昨天的日报可编辑");
        }
    }
}

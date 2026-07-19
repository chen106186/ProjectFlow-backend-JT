package com.jitong.projectflow.common.api;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;

import java.time.LocalDate;

public final class DateRangeValidator {

    private DateRangeValidator() {}

    public static void validate(LocalDate startDate, LocalDate endDate, String label) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "结束日期不能早于开始日期");
        }
    }
}

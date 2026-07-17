package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;

public final class CurrentUserContext {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> DEPT_NAME_HOLDER = new ThreadLocal<>();

    private static final String GM_OFFICE_NAME = "总经办";

    private CurrentUserContext() {}

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static void setDeptName(String deptName) {
        DEPT_NAME_HOLDER.set(deptName);
    }

    public static String getDeptName() {
        return DEPT_NAME_HOLDER.get();
    }

    public static boolean isGmOffice() {
        return GM_OFFICE_NAME.equals(DEPT_NAME_HOLDER.get());
    }

    public static Long userId() {
        Long id = HOLDER.get();
        if (id == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return id;
    }

    public static Long userIdOrNull() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
        DEPT_NAME_HOLDER.remove();
    }
}

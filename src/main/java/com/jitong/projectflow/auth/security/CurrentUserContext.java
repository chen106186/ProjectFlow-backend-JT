package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;

public final class CurrentUserContext {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {}

    public static void set(Long userId) {
        HOLDER.set(userId);
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
    }
}

package com.jitong.projectflow.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class PageUtils {
    private static final long DEFAULT_PAGE_NO = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 200L;

    private PageUtils() {
    }

    public static <T> Page<T> toPage(PageQuery query) {
        long pageNo = query == null || query.getPageNo() == null ? DEFAULT_PAGE_NO : Math.max(DEFAULT_PAGE_NO, query.getPageNo());
        long pageSize = query == null || query.getPageSize() == null ? DEFAULT_PAGE_SIZE : Math.max(1L, Math.min(MAX_PAGE_SIZE, query.getPageSize()));
        return new Page<>(pageNo, pageSize);
    }

    public static <T> PageResult<T> toResult(Page<?> page, java.util.List<T> records) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }
}

package com.jitong.projectflow.common.api;

import java.util.List;

public record PageResult<T>(long total, long pageNo, long pageSize, List<T> records) {
}

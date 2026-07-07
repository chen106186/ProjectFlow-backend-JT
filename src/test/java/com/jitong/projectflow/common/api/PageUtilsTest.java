package com.jitong.projectflow.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilsTest {
    @Test
    void createsPageWithDefaults() {
        PageQuery query = new PageQuery();

        Page<Object> page = PageUtils.toPage(query);

        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(20);
    }

    @Test
    void capsPageSizeAtTwoHundred() {
        PageQuery query = new PageQuery();
        query.setPageNo(2L);
        query.setPageSize(500L);

        Page<Object> page = PageUtils.toPage(query);

        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(200);
    }
}

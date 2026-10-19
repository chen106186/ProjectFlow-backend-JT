package com.jitong.projectflow.common.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessNumberServiceTest {
    private final JdbcDataSource dataSource = new JdbcDataSource();
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaction;
    private final BusinessNumberService numbers;

    BusinessNumberServiceTest() {
        dataSource.setURL("jdbc:h2:mem:number_test_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE pf_business_number (business_type VARCHAR(32) PRIMARY KEY, next_value BIGINT NOT NULL)");
        jdbcTemplate.update("INSERT INTO pf_business_number VALUES ('BUG', 41), ('REQUIREMENT', 7)");
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        numbers = new BusinessNumberService(jdbcTemplate);
    }

    @Test
    void concurrentTransactionsAllocateDistinctNumbers() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        try {
            List<Future<Long>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return transaction.execute(status -> numbers.nextBugNo());
                }));
            }
            start.countDown();
            var allocated = new HashSet<Long>();
            for (Future<Long> result : results) {
                allocated.add(result.get());
            }
            assertThat(allocated).containsExactlyInAnyOrder(41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L);
            Long requirementNo = transaction.execute(status -> numbers.nextRequirementNo());
            assertThat(requirementNo).isEqualTo(7L);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void allocationRequiresTransactionAndRollbackDoesNotConsumeNumber() {
        assertThatThrownBy(numbers::nextBugNo).isInstanceOf(IllegalStateException.class);
        transaction.execute(status -> {
            assertThat(numbers.nextBugNo()).isEqualTo(41L);
            status.setRollbackOnly();
            return null;
        });
        Long reusedNumber = transaction.execute(status -> numbers.nextBugNo());
        assertThat(reusedNumber).isEqualTo(41L);
    }
}

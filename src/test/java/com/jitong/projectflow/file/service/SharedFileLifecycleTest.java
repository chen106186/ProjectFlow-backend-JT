package com.jitong.projectflow.file.service;

import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Sql(statements = {
        "CREATE TABLE IF NOT EXISTS pf_file (id BIGINT PRIMARY KEY, business_type VARCHAR(64), business_id BIGINT, original_name VARCHAR(255), content_type VARCHAR(128), file_size BIGINT, version_no VARCHAR(64), storage_location VARCHAR(64), file_category VARCHAR(64), folder_id BIGINT, storage_type VARCHAR(64), storage_key VARCHAR(512), uploader_id BIGINT, uploaded_at TIMESTAMP, deleted INT DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS pf_file_folder (id BIGINT PRIMARY KEY, business_type VARCHAR(64), business_id BIGINT, name VARCHAR(255), created_by BIGINT, created_at TIMESTAMP, deleted INT DEFAULT 0)",
        "DELETE FROM pf_file",
        "DELETE FROM pf_file_folder",
        "INSERT INTO pf_file(id, business_type, business_id, storage_type, storage_key, deleted) VALUES (1, 'DAILY_REPORT', 10, 'LOCAL', 'shared.pdf', 0), (2, 'PROJECT', 20, 'LOCAL', 'shared.pdf', 0)"
})
class SharedFileLifecycleTest {
    @Autowired FileService files;
    @Autowired FileMetadataMapper metadata;
    @Autowired PlatformTransactionManager transactions;
    @MockBean FileStorageService storage;
    @MockBean BusinessAccessService access;
    @MockBean FileBusinessAccessService business;

    @Test
    void onlyLastReferenceRemovesObjectAndOnlyAfterCommit() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> files.delete(1L));
        assertThat(metadata.selectById(1L)).isNull();
        assertThat(metadata.selectById(2L)).isNotNull();
        verify(storage, never()).delete(any());

        transaction.executeWithoutResult(status -> {
            files.delete(2L);
            verify(storage, never()).delete(any());
        });
        assertThat(metadata.selectById(2L)).isNull();
        verify(storage).delete("shared.pdf");
    }

    @Test
    void rollbackDoesNotRemoveMetadataOrObject() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> {
            files.delete(1L);
            status.setRollbackOnly();
        });
        assertThat(metadata.selectById(1L)).isNotNull();
        verify(storage, never()).delete(any());
    }

    @Test
    void deletingOwningBusinessPurgesItsReferenceWithoutBreakingMirror() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.executeWithoutResult(status -> files.purgeForBusiness("DAILY_REPORT", 10L));
        assertThat(metadata.selectById(1L)).isNull();
        assertThat(metadata.selectById(2L)).isNotNull();
        verify(storage, never()).delete(any());

        transaction.executeWithoutResult(status -> files.purgeForBusiness("PROJECT", 20L));
        assertThat(metadata.selectById(2L)).isNull();
        verify(storage).delete("shared.pdf");
    }
}

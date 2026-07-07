package com.jitong.projectflow.notice.service;

import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.dto.NoticeResponse;
import com.jitong.projectflow.notice.entity.NoticeEntity;
import com.jitong.projectflow.notice.mapper.NoticeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Sql(statements = {
    "CREATE TABLE IF NOT EXISTS pf_notice (" +
    "  id BIGINT PRIMARY KEY," +
    "  receiver_id BIGINT NOT NULL," +
    "  notice_type VARCHAR(64) NOT NULL," +
    "  title VARCHAR(200) NOT NULL," +
    "  content VARCHAR(1000) NOT NULL," +
    "  business_type VARCHAR(64) NULL," +
    "  business_id BIGINT NULL," +
    "  read_flag TINYINT NOT NULL DEFAULT 0," +
    "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
    "  read_at DATETIME NULL," +
    "  deleted TINYINT NOT NULL DEFAULT 0" +
    ")"
})
class NoticeServiceTest {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeMapper noticeMapper;

    private static final Long RECEIVER_ID = 100L;

    @Test
    void create_insertsNoticeWithReadFlagZero() {
        noticeService.create(RECEIVER_ID, NoticeType.SYSTEM, "Test Title", "Test Content", null, null);

        List<NoticeResponse> notices = noticeService.list(RECEIVER_ID);
        assertThat(notices).hasSize(1);
        NoticeResponse notice = notices.get(0);
        assertThat(notice.getTitle()).isEqualTo("Test Title");
        assertThat(notice.getContent()).isEqualTo("Test Content");
        assertThat(notice.getNoticeType()).isEqualTo("SYSTEM");
        assertThat(notice.isRead()).isFalse();
    }

    @Test
    void unreadCount_returnsCorrectCount() {
        noticeService.create(RECEIVER_ID, NoticeType.TASK_ASSIGNED, "Task 1", "Content 1", null, null);
        noticeService.create(RECEIVER_ID, NoticeType.BUG_ASSIGNED, "Bug 1", "Content 2", null, null);
        noticeService.create(RECEIVER_ID, NoticeType.SYSTEM, "System 1", "Content 3", null, null);

        long count = noticeService.unreadCount(RECEIVER_ID);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void markRead_setsReadFlagAndReadAt() {
        noticeService.create(RECEIVER_ID, NoticeType.SYSTEM, "Mark Test", "Content", null, null);
        List<NoticeResponse> notices = noticeService.list(RECEIVER_ID);
        assertThat(notices).hasSize(1);
        Long noticeId = notices.get(0).getId();

        assertThat(noticeService.unreadCount(RECEIVER_ID)).isEqualTo(1);

        noticeService.markRead(RECEIVER_ID, noticeId);

        NoticeEntity updated = noticeMapper.selectById(noticeId);
        assertThat(updated.getReadFlag()).isEqualTo(1);
        assertThat(updated.getReadAt()).isNotNull();
        assertThat(noticeService.unreadCount(RECEIVER_ID)).isEqualTo(0);
    }

    @Test
    void markRead_throwsNotFound_whenWrongReceiver() {
        noticeService.create(RECEIVER_ID, NoticeType.SYSTEM, "Other User Notice", "Content", null, null);
        List<NoticeResponse> notices = noticeService.list(RECEIVER_ID);
        Long noticeId = notices.get(0).getId();

        assertThatThrownBy(() -> noticeService.markRead(999L, noticeId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Notice not found");
    }

    @Test
    void markAllRead_marksAllUnreadNoticesAsRead() {
        noticeService.create(RECEIVER_ID, NoticeType.TASK_ASSIGNED, "Task 1", "Content 1", null, null);
        noticeService.create(RECEIVER_ID, NoticeType.BUG_ASSIGNED, "Bug 1", "Content 2", null, null);
        noticeService.create(RECEIVER_ID, NoticeType.SYSTEM, "System 1", "Content 3", null, null);

        assertThat(noticeService.unreadCount(RECEIVER_ID)).isEqualTo(3);

        noticeService.markAllRead(RECEIVER_ID);

        assertThat(noticeService.unreadCount(RECEIVER_ID)).isEqualTo(0);

        List<NoticeResponse> notices = noticeService.list(RECEIVER_ID);
        assertThat(notices).allMatch(NoticeResponse::isRead);
        assertThat(notices).allMatch(n -> n.getReadAt() != null);
    }
}

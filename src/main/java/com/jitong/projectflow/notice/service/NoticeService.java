package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.dto.NoticeResponse;
import com.jitong.projectflow.notice.entity.NoticeEntity;
import com.jitong.projectflow.notice.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public void create(Long receiverId, NoticeType type, String title, String content,
                       String businessType, Long businessId) {
        NoticeEntity entity = new NoticeEntity();
        entity.setReceiverId(receiverId);
        entity.setNoticeType(type.name());
        entity.setTitle(title);
        entity.setContent(content);
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setReadFlag(0);
        entity.setCreatedAt(LocalDateTime.now());
        noticeMapper.insert(entity);
    }

    public long unreadCount(Long receiverId) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeEntity::getReceiverId, receiverId)
               .eq(NoticeEntity::getReadFlag, 0);
        return noticeMapper.selectCount(wrapper);
    }

    public void markRead(Long receiverId, Long noticeId) {
        NoticeEntity entity = noticeMapper.selectById(noticeId);
        if (entity == null || !entity.getReceiverId().equals(receiverId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        entity.setReadFlag(1);
        entity.setReadAt(LocalDateTime.now());
        noticeMapper.updateById(entity);
    }

    public void markAllRead(Long receiverId) {
        LambdaUpdateWrapper<NoticeEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NoticeEntity::getReceiverId, receiverId)
               .eq(NoticeEntity::getReadFlag, 0)
               .set(NoticeEntity::getReadFlag, 1)
               .set(NoticeEntity::getReadAt, LocalDateTime.now());
        noticeMapper.update(null, wrapper);
    }

    public List<NoticeResponse> list(Long receiverId) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeEntity::getReceiverId, receiverId)
               .orderByDesc(NoticeEntity::getCreatedAt);
        List<NoticeEntity> entities = noticeMapper.selectList(wrapper);
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private NoticeResponse toResponse(NoticeEntity entity) {
        return NoticeResponse.builder()
                .id(entity.getId())
                .noticeType(entity.getNoticeType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .businessType(entity.getBusinessType())
                .businessId(entity.getBusinessId())
                .read(entity.getReadFlag() != null && entity.getReadFlag() == 1)
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}

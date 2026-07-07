package com.jitong.projectflow.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pf_notice")
public class NoticeEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long receiverId;

    private String noticeType;

    private String title;

    private String content;

    private String businessType;

    private Long businessId;

    private Integer readFlag;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @TableLogic
    private Integer deleted;
}

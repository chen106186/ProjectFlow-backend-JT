package com.jitong.projectflow.bug.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jitong.projectflow.bug.entity.BugEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BugMapper extends BaseMapper<BugEntity> {}

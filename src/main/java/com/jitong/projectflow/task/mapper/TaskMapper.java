package com.jitong.projectflow.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {}

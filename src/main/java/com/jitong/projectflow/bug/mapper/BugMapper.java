package com.jitong.projectflow.bug.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jitong.projectflow.bug.entity.BugEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BugMapper extends BaseMapper<BugEntity> {

    @Select("SELECT COALESCE(MAX(bug_no), 0) FROM pf_bug")
    Long selectMaxBugNo();
}

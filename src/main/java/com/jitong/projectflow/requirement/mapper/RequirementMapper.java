package com.jitong.projectflow.requirement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequirementMapper extends BaseMapper<RequirementEntity> {

    @Select("SELECT COALESCE(MAX(requirement_no), 0) FROM pf_requirement")
    Long selectMaxRequirementNo();
}

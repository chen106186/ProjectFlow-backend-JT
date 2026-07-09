package com.jitong.projectflow.project.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectParticipantMapper {

    @Select("SELECT user_id FROM pf_project_participant WHERE project_id = #{projectId}")
    List<Long> selectUserIdsByProjectId(@Param("projectId") Long projectId);

    @Delete("DELETE FROM pf_project_participant WHERE project_id = #{projectId}")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Insert("<script>" +
            "INSERT INTO pf_project_participant(project_id, user_id) VALUES " +
            "<foreach collection='userIds' item='uid' separator=','>(#{projectId}, #{uid})</foreach>" +
            "</script>")
    void batchInsert(@Param("projectId") Long projectId, @Param("userIds") List<Long> userIds);
}

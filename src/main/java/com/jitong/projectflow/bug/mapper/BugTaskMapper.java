package com.jitong.projectflow.bug.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BugTaskMapper {
    @Insert("INSERT IGNORE INTO pf_bug_task(bug_id, task_id) VALUES(#{bugId}, #{taskId})")
    void insert(@Param("bugId") Long bugId, @Param("taskId") Long taskId);

    @Delete("DELETE FROM pf_bug_task WHERE bug_id = #{bugId}")
    void deleteByBugId(@Param("bugId") Long bugId);

    @Select("SELECT task_id FROM pf_bug_task WHERE bug_id = #{bugId}")
    List<Long> findTaskIdsByBugId(@Param("bugId") Long bugId);
}

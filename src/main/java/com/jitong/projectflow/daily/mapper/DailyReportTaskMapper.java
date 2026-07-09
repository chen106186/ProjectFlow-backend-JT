package com.jitong.projectflow.daily.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DailyReportTaskMapper {

    @Insert("INSERT IGNORE INTO pf_daily_report_task(report_id, task_id) VALUES(#{reportId}, #{taskId})")
    void insert(@Param("reportId") Long reportId, @Param("taskId") Long taskId);

    @Delete("DELETE FROM pf_daily_report_task WHERE report_id = #{reportId}")
    void deleteByReportId(@Param("reportId") Long reportId);

    @Select("SELECT task_id FROM pf_daily_report_task WHERE report_id = #{reportId}")
    List<Long> findTaskIdsByReportId(@Param("reportId") Long reportId);
}

package com.jitong.projectflow.system.mapper;

import com.jitong.projectflow.system.entity.RoleMenuEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMenuMapper {
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insertRelation(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select({
            "<script>",
            "SELECT DISTINCT menu_id AS menuId FROM sys_role_menu WHERE role_id IN",
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach>",
            "</script>"
    })
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}

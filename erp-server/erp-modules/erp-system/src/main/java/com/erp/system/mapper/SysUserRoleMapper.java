package com.erp.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户-角色关联 Mapper。
 * <p>
 * 纯关联表，只有增删没有改，直接用注解写 SQL 比建 XML 更省事
 * （XML 的价值在于动态条件拼装，这里没有动态条件）。
 * <p>
 * 注意：这张表没有逻辑删除字段，deleted 就是物理删除一行 —— 见建表脚本里的说明。
 */
@Mapper
public interface SysUserRoleMapper {

    /** 清空某用户的角色关联。分配角色时先清后插是最简单的做法 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 删除某角色的全部关联，删除角色前调用 */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 新增关联。
     * <p>
     * 用 INSERT IGNORE：重复分配同一角色不报错。否则保存用户时前端把已勾选的
     * 角色原样传回来，就会撞主键报"数据已存在"。
     */
    @Insert("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 统计有多少用户被分配了该角色。
     * <p>
     * 删角色前用它给出准确提示："该角色已分配给 3 个用户"，
     * 比笼统的"删除失败"有用得多，也避免出现"角色没了、用户还挂着一个不存在的 roleId"的脏数据。
     */
    @Select("SELECT COUNT(1) FROM sys_user_role WHERE role_id = #{roleId}")
    int countByRoleId(@Param("roleId") Long roleId);
}

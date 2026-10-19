package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门 Mapper。
 * <p>
 * 部门的查询都是单表条件查询，直接用 BaseMapper 的 selectList(Wrapper) 即可，
 * 不需要写 XML —— 这也是判断"要不要写 XML"的标准：单表能表达就不用写。
 * <p>
 * 唯一例外是 {@link #selectUserCountByDeptId(Long)}：删部门前要统计"这个部门下挂了多少用户"，
 * 而用户表不归本 Mapper 的实体管，用一条注解 SQL 表达比拉一个 XML 更省事。
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 统计归属该部门的用户数（不含逻辑删除的用户）。
     * <p>
     * 有用户归属的部门不允许删除，否则这些用户的 dept_id 会指向一个不存在的部门，
     * 列表页联表带出的 departmentName 变成空，排查时很难联想到"是部门被删了"。
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE is_deleted = 0 AND dept_id = #{deptId}")
    long selectUserCountByDeptId(@Param("deptId") Long deptId);
}

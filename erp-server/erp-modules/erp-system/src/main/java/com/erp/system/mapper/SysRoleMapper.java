package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询用户拥有的角色（只含启用状态），构建 LoginUser 用 */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /** 查询用户的角色 ID 列表，用户编辑页回显勾选用 */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}

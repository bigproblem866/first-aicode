package com.erp.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.dto.MdmMaterialQuery;
import com.erp.mdm.domain.vo.MdmMaterialVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物料 Mapper。
 * <p>
 * 单表增删改查用 BaseMapper 自带方法；只有需要联表翻译名称时才写 XML。
 * <p>
 * ⚠ 自己写的 XML 不会自动追加逻辑删除条件，必须手写 {@code m.is_deleted = 0}。
 */
@Mapper
public interface MdmMaterialMapper extends BaseMapper<MdmMaterial> {

    /**
     * 物料分页（联 mdm_material_category 取分类名、联 sys_dict_data 取单位/类型名称）。
     * <p>
     * IPage 必须是第一个参数，否则分页静默失效。
     */
    IPage<MdmMaterialVo> selectMaterialPage(IPage<?> page, @Param("q") MdmMaterialQuery query);

    /** 物料下拉（单据选料用），keyword 匹配编码或名称 */
    List<MdmMaterialVo> selectMaterialOptions(@Param("keyword") String keyword);
}

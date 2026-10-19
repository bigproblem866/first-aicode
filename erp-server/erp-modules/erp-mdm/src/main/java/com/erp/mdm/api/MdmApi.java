package com.erp.mdm.api;

import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.mdm.domain.MdmWarehouse;

import java.util.Collection;
import java.util.List;

/**
 * 主数据对外服务接口 —— 跨模块调用边界。
 * <p>
 * <b>这是本工程「模块边界」规则唯一的对外出口</b>：
 * erp-inventory / erp-purchase 需要物料、仓库信息时，只能注入本接口，
 * 绝不允许直接注入 mdm 的 Mapper 或去 join mdm_* 表。
 * 好处是将来把 mdm 拆成独立微服务时，只需把本接口的实现改成远程调用，
 * 调用方一行不用动。
 * <p>
 * 接口只暴露「读」能力：业务模块无权修改主数据，所以这里没有任何写方法。
 */
public interface MdmApi {

    /** 按 ID 取物料，不存在（或已逻辑删除）返回 null */
    MdmMaterial getMaterialById(Long id);

    /** 按 ID 集合批量取物料，入参为空返回空列表（供列表页批量填充名称，避免 N+1） */
    List<MdmMaterial> listMaterialsByIds(Collection<Long> ids);

    /** 按 ID 取仓库，不存在返回 null */
    MdmWarehouse getWarehouseById(Long id);

    /** 按 ID 取供应商，不存在返回 null（采购模块列表页展示供应商名用） */
    MdmSupplier getSupplierById(Long id);

    /** 按 ID 集合批量取供应商（供列表页批量填充名称，避免 N+1） */
    List<MdmSupplier> listSuppliersByIds(Collection<Long> ids);

    /**
     * 按关键字（物料编码或名称，模糊匹配）搜索物料 ID。
     * <p>
     * 存在这个方法的理由：别的模块需要按"物料编码"筛选自己的数据，
     * 但不能 join mdm_material。走本接口把编码解析成 ID 后按 ID 过滤，
     * 边界没有被破坏，将来改成远程调用也只是多一跳网络。
     *
     * @return 匹配的物料 ID；无匹配返回空列表（调用方要按"查不到"处理，不能当成"不过滤"）
     */
    List<Long> searchMaterialIdsByKeyword(String keyword);

    /** 物料是否存在（有效），供写单据前校验引用合法性 */
    boolean existsMaterial(Long id);
}

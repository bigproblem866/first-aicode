package com.erp.mdm.api.impl;

import com.erp.mdm.api.MdmApi;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.mdm.domain.MdmWarehouse;
import com.erp.mdm.domain.vo.MdmMaterialVo;
import com.erp.mdm.mapper.MdmMaterialMapper;
import com.erp.mdm.mapper.MdmSupplierMapper;
import com.erp.mdm.mapper.MdmWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * {@link MdmApi} 实现。
 * <p>
 * 注意本类在 mdm 模块内部，可以直接用本模块的 Mapper；
 * 别处（inventory / purchase）拿到的只是 {@link MdmApi} 接口，
 * 看不到这些 Mapper，模块边界因此在编译期就被强制住。
 */
@Service
@RequiredArgsConstructor
public class MdmApiImpl implements MdmApi {

    private final MdmMaterialMapper materialMapper;
    private final MdmWarehouseMapper warehouseMapper;
    private final MdmSupplierMapper supplierMapper;

    @Override
    public MdmMaterial getMaterialById(Long id) {
        if (id == null) {
            return null;
        }
        return materialMapper.selectById(id);
    }

    @Override
    public List<MdmMaterial> listMaterialsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        // selectBatchIds 自动带逻辑删除条件（is_deleted = 0），不需要手写
        return materialMapper.selectBatchIds(ids);
    }

    @Override
    public MdmWarehouse getWarehouseById(Long id) {
        if (id == null) {
            return null;
        }
        return warehouseMapper.selectById(id);
    }

    @Override
    public MdmSupplier getSupplierById(Long id) {
        if (id == null) {
            return null;
        }
        return supplierMapper.selectById(id);
    }

    @Override
    public List<MdmSupplier> listSuppliersByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return supplierMapper.selectBatchIds(ids);
    }

    @Override
    public boolean existsMaterial(Long id) {
        return getMaterialById(id) != null;
    }

    @Override
    public List<Long> searchMaterialIdsByKeyword(String keyword) {
        // 空关键字返回空列表而不是"全部物料"：
        // 调用方（库存流水按编码筛选）拿到空列表会返回空结果页，
        // 而如果这里返回全部 ID，就等于"筛选条件被忽略"，用户会看到不该看到的数据。
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return materialMapper.selectMaterialOptions(keyword.trim()).stream()
                .map(MdmMaterialVo::getId)
                .filter(Objects::nonNull)
                .toList();
    }
}

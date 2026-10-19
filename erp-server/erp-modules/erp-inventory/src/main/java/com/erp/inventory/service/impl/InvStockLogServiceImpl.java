package com.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageResult;
import com.erp.inventory.domain.InvStockLog;
import com.erp.inventory.domain.dto.InvStockLogQuery;
import com.erp.inventory.domain.vo.InvStockLogVo;
import com.erp.inventory.mapper.InvStockLogMapper;
import com.erp.inventory.service.IInvStockLogService;
import com.erp.mdm.api.MdmApi;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmWarehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存流水服务实现。名称同样通过 {@link MdmApi} 填充，不 join mdm 表。
 */
@Service
@RequiredArgsConstructor
public class InvStockLogServiceImpl implements IInvStockLogService {

    private final InvStockLogMapper stockLogMapper;
    private final MdmApi mdmApi;

    @Override
    public PageResult<InvStockLogVo> pageList(InvStockLogQuery query) {
        Page<InvStockLog> page = query.toPage();
        LambdaQueryWrapper<InvStockLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getWarehouseId() != null, InvStockLog::getWarehouseId, query.getWarehouseId());
        wrapper.eq(query.getMaterialId() != null, InvStockLog::getMaterialId, query.getMaterialId());
        wrapper.eq(query.getBizType() != null, InvStockLog::getBizType, query.getBizType());
        wrapper.ge(query.getBizDateStart() != null, InvStockLog::getBizDate, query.getBizDateStart());
        wrapper.le(query.getBizDateEnd() != null, InvStockLog::getBizDate, query.getBizDateEnd());

        // 物料编码筛选：编码在 mdm 模块，本模块不能 join 它的表，
        // 所以先通过 MdmApi 把编码解析成 ID 列表，再按 ID 过滤。
        // 解析不到任何物料时直接返回空页 —— 否则条件会被"忽略"，
        // 用户以为筛出了结果，实际看到的是全部数据。
        if (query.getMaterialCode() != null && !query.getMaterialCode().isBlank()) {
            List<Long> materialIds = mdmApi.searchMaterialIdsByKeyword(query.getMaterialCode().trim());
            if (materialIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(InvStockLog::getMaterialId, materialIds);
        }

        wrapper.orderByDesc(InvStockLog::getId);

        IPage<InvStockLog> result = stockLogMapper.selectPage(page, wrapper);
        List<InvStockLog> records = result.getRecords();

        Set<Long> materialIds = records.stream().map(InvStockLog::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MdmMaterial> materials = materialIds.isEmpty() ? new HashMap<>()
                : mdmApi.listMaterialsByIds(materialIds).stream()
                .collect(Collectors.toMap(MdmMaterial::getId, m -> m, (a, b) -> a));
        Map<Long, MdmWarehouse> warehouses = new HashMap<>();
        for (Long id : records.stream().map(InvStockLog::getWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet())) {
            MdmWarehouse warehouse = mdmApi.getWarehouseById(id);
            if (warehouse != null) {
                warehouses.put(id, warehouse);
            }
        }

        List<InvStockLogVo> rows = records.stream().map(log -> {
            InvStockLogVo vo = new InvStockLogVo();
            vo.setId(log.getId());
            vo.setWarehouseId(log.getWarehouseId());
            vo.setMaterialId(log.getMaterialId());
            vo.setBizType(log.getBizType());
            vo.setQty(log.getQty());
            vo.setBeforeQty(log.getBeforeQty());
            vo.setAfterQty(log.getAfterQty());
            vo.setSourceType(log.getSourceType());
            vo.setSourceId(log.getSourceId());
            vo.setSourceNo(log.getSourceNo());
            vo.setBizDate(log.getBizDate());
            vo.setRemark(log.getRemark());
            vo.setCreateBy(log.getCreateBy());
            vo.setCreateTime(log.getCreateTime());
            MdmMaterial material = materials.get(log.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
                vo.setUnit(material.getUnit());
            }
            MdmWarehouse warehouse = warehouses.get(log.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getWarehouseName());
            }
            return vo;
        }).toList();

        return PageResult.of(result, rows);
    }
}

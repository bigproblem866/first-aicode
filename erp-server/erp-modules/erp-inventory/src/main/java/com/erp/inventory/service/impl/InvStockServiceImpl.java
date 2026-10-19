package com.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.framework.security.SecurityUtils;
import com.erp.inventory.api.dto.InboundItem;
import com.erp.inventory.api.dto.InboundRequest;
import com.erp.inventory.constant.InvConstants;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.domain.InvOrderDetail;
import com.erp.inventory.domain.InvStock;
import com.erp.inventory.domain.InvStockLog;
import com.erp.inventory.domain.dto.InvStockQuery;
import com.erp.inventory.domain.vo.InvStockVo;
import com.erp.inventory.mapper.InvOrderDetailMapper;
import com.erp.inventory.mapper.InvOrderMapper;
import com.erp.inventory.mapper.InvStockLogMapper;
import com.erp.inventory.mapper.InvStockMapper;
import com.erp.inventory.service.IInvStockService;
import com.erp.inventory.service.InvOrderNoGenerator;
import com.erp.mdm.api.MdmApi;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmWarehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存台账服务实现。
 * <p>
 * 跨模块取物料/仓库信息一律走 {@link MdmApi}，本类里没有一句 SQL 触碰 mdm_* 表。
 * 列表是「先分页查台账 → 批量取主数据 → 组装 VO」，而不是 join —— 这正是本项目
 * 演示「模块边界」的地方。
 */
@Service
@RequiredArgsConstructor
public class InvStockServiceImpl implements IInvStockService {

    private final InvStockMapper stockMapper;
    private final InvStockLogMapper stockLogMapper;
    private final InvOrderMapper orderMapper;
    private final InvOrderDetailMapper orderDetailMapper;
    private final InvOrderNoGenerator orderNoGenerator;
    /** 跨模块依赖：只允许注入 mdm 暴露的接口，绝不注入 mdm 的 Mapper */
    private final MdmApi mdmApi;

    @Override
    public PageResult<InvStockVo> pageList(InvStockQuery query) {
        Page<InvStock> page = query.toPage();
        LambdaQueryWrapper<InvStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getWarehouseId() != null, InvStock::getWarehouseId, query.getWarehouseId());
        wrapper.eq(query.getMaterialId() != null, InvStock::getMaterialId, query.getMaterialId());
        wrapper.orderByDesc(InvStock::getId);

        IPage<InvStock> result = stockMapper.selectPage(page, wrapper);
        List<InvStock> records = result.getRecords();
        Map<Long, MdmMaterial> materials = loadMaterials(records.stream().map(InvStock::getMaterialId).toList());
        Map<Long, MdmWarehouse> warehouses = loadWarehouses(records.stream().map(InvStock::getWarehouseId).toList());

        List<InvStockVo> rows = records.stream().map(stock -> {
            InvStockVo vo = new InvStockVo();
            vo.setId(stock.getId());
            vo.setWarehouseId(stock.getWarehouseId());
            vo.setMaterialId(stock.getMaterialId());
            vo.setQty(stock.getQty());
            vo.setRemark(stock.getRemark());
            vo.setCreateBy(stock.getCreateBy());
            vo.setCreateTime(stock.getCreateTime());
            vo.setUpdateBy(stock.getUpdateBy());
            vo.setUpdateTime(stock.getUpdateTime());
            MdmMaterial material = materials.get(stock.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
                vo.setUnit(material.getUnit());
            }
            MdmWarehouse warehouse = warehouses.get(stock.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getWarehouseName());
            }
            return vo;
        }).toList();

        return PageResult.of(result, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void increaseStock(Long warehouseId, Long materialId, BigDecimal qty, String bizType,
                              String sourceType, Long sourceId, String sourceNo, LocalDate bizDate, String remark) {
        if (qty == null || qty.signum() <= 0) {
            throw new ServiceException("入库数量必须大于 0");
        }
        String operator = SecurityUtils.getUsernameOrSystem();
        // UPSERT：台账行不存在时自动创建，存在则累加，均由数据库一次完成
        stockMapper.increaseQty(warehouseId, materialId, qty, operator);
        // 事务内该行已被本事务持有写锁，随后读到的就是本次变动后的值
        BigDecimal after = currentQty(warehouseId, materialId);
        BigDecimal before = after.subtract(qty);
        writeLog(warehouseId, materialId, bizType, qty, before, after,
                sourceType, sourceId, sourceNo, bizDate, remark);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void decreaseStock(Long warehouseId, Long materialId, BigDecimal qty, String bizType,
                              String sourceType, Long sourceId, String sourceNo, LocalDate bizDate, String remark) {
        if (qty == null || qty.signum() <= 0) {
            throw new ServiceException("出库数量必须大于 0");
        }
        String operator = SecurityUtils.getUsernameOrSystem();
        // 关键：带条件的原子扣减，影响行数为 0 说明库存不足，直接抛异常让整个事务回滚
        int rows = stockMapper.decreaseQty(warehouseId, materialId, qty, operator);
        if (rows == 0) {
            throw new ServiceException("库存不足，物料" + materialLabel(materialId) + "当前库存不足");
        }
        BigDecimal after = currentQty(warehouseId, materialId);
        BigDecimal before = after.add(qty);
        // 流水里出库记为负数，与「入库为正、出库为负」的约定一致
        writeLog(warehouseId, materialId, bizType, qty.negate(), before, after,
                sourceType, sourceId, sourceNo, bizDate, remark);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public InvOrder createInboundFromReceipt(InboundRequest request) {
        if (request == null || request.getWarehouseId() == null) {
            throw new ServiceException("入库仓库不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ServiceException("入库明细不能为空");
        }
        LocalDate bizDate = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();

        // 1) 建入库单（草稿），来源指向采购收货单
        InvOrder order = new InvOrder();
        order.setOrderNo(orderNoGenerator.next(InvConstants.PREFIX_IN));
        order.setOrderType(InvOrder.TYPE_IN);
        order.setWarehouseId(request.getWarehouseId());
        order.setBizDate(bizDate);
        order.setStatus(InvOrder.STATUS_DRAFT);
        order.setSourceType(request.getSourceType());
        order.setSourceNo(request.getSourceNo());
        order.setRemark(request.getRemark());
        orderMapper.insert(order);

        // 2) 写明细，金额后端算
        for (InboundItem item : request.getItems()) {
            InvOrderDetail detail = new InvOrderDetail();
            detail.setOrderId(order.getId());
            detail.setMaterialId(item.getMaterialId());
            detail.setQty(item.getQty());
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            detail.setPrice(price);
            detail.setAmount(price.multiply(item.getQty()));
            detail.setRemark(item.getRemark());
            orderDetailMapper.insert(detail);
        }

        // 3) 立即入库：加台账 + 写流水（source 指向本张入库单，便于从流水点回单据）
        for (InboundItem item : request.getItems()) {
            increaseStock(order.getWarehouseId(), item.getMaterialId(), item.getQty(),
                    InvConstants.BIZ_IN, InvConstants.SOURCE_INV_ORDER,
                    order.getId(), order.getOrderNo(), bizDate, request.getRemark());
        }

        // 4) 标记为已审核
        applyAudited(order.getId());
        order.setStatus(InvOrder.STATUS_AUDITED);
        order.setAuditBy(SecurityUtils.getUsernameOrSystem());
        order.setAuditTime(LocalDateTime.now());
        return order;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 读台账当前数量（事务内读，行已被本事务锁定，值可信） */
    private BigDecimal currentQty(Long warehouseId, Long materialId) {
        InvStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                .eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getMaterialId, materialId));
        return stock == null || stock.getQty() == null ? BigDecimal.ZERO : stock.getQty();
    }

    /** 写一条流水。台账的每一次变动都必须调用它，这是库存可对账的前提 */
    private void writeLog(Long warehouseId, Long materialId, String bizType, BigDecimal qty,
                          BigDecimal before, BigDecimal after, String sourceType, Long sourceId,
                          String sourceNo, LocalDate bizDate, String remark) {
        InvStockLog log = new InvStockLog();
        log.setWarehouseId(warehouseId);
        log.setMaterialId(materialId);
        log.setBizType(bizType);
        log.setQty(qty);
        log.setBeforeQty(before);
        log.setAfterQty(after);
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setSourceNo(sourceNo);
        log.setBizDate(bizDate);
        log.setRemark(remark);
        stockLogMapper.insert(log);
    }

    private void applyAudited(Long orderId) {
        InvOrder update = new InvOrder();
        update.setId(orderId);
        update.setStatus(InvOrder.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getUsernameOrSystem());
        update.setAuditTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    private String materialLabel(Long materialId) {
        MdmMaterial material = mdmApi.getMaterialById(materialId);
        return material == null ? String.valueOf(materialId) : material.getMaterialName();
    }

    private Map<Long, MdmMaterial> loadMaterials(List<Long> materialIds) {
        Set<Long> ids = materialIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return mdmApi.listMaterialsByIds(ids).stream()
                .collect(Collectors.toMap(MdmMaterial::getId, m -> m, (a, b) -> a));
    }

    private Map<Long, MdmWarehouse> loadWarehouses(List<Long> warehouseIds) {
        Set<Long> ids = warehouseIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MdmWarehouse> result = new HashMap<>();
        for (Long id : ids) {
            MdmWarehouse warehouse = mdmApi.getWarehouseById(id);
            if (warehouse != null) {
                result.put(id, warehouse);
            }
        }
        return result;
    }
}

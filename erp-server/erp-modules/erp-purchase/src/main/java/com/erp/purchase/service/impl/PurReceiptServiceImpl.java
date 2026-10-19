package com.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.framework.security.SecurityUtils;
import com.erp.inventory.api.dto.InboundItem;
import com.erp.inventory.api.dto.InboundRequest;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.service.IInvStockService;
import com.erp.mdm.api.MdmApi;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.mdm.domain.MdmWarehouse;
import com.erp.purchase.domain.PurOrder;
import com.erp.purchase.domain.PurOrderDetail;
import com.erp.purchase.domain.PurReceipt;
import com.erp.purchase.domain.PurReceiptDetail;
import com.erp.purchase.domain.dto.PurReceiptDetailDto;
import com.erp.purchase.domain.dto.PurReceiptDto;
import com.erp.purchase.domain.dto.PurReceiptQuery;
import com.erp.purchase.domain.vo.PurReceiptVo;
import com.erp.purchase.mapper.PurOrderDetailMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.mapper.PurReceiptDetailMapper;
import com.erp.purchase.mapper.PurReceiptMapper;
import com.erp.purchase.service.IPurReceiptService;
import com.erp.purchase.service.PurNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采购收货单服务实现。
 */
@Service
@RequiredArgsConstructor
public class PurReceiptServiceImpl implements IPurReceiptService {

    /** 入库单来源类型标识 */
    private static final String SOURCE_PUR_RECEIPT = "PUR_RECEIPT";

    private final PurReceiptMapper receiptMapper;
    private final PurReceiptDetailMapper receiptDetailMapper;
    private final PurOrderMapper orderMapper;
    private final PurOrderDetailMapper orderDetailMapper;
    private final PurNoGenerator noGenerator;
    /** 跨模块依赖：只注入库存模块暴露的接口，绝不操作 inv_* 表 */
    private final IInvStockService stockService;
    private final MdmApi mdmApi;

    @Override
    public PageResult<PurReceiptVo> pageList(PurReceiptQuery query) {
        Page<PurReceipt> page = query.toPage();
        LambdaQueryWrapper<PurReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getReceiptNo() != null, PurReceipt::getReceiptNo, query.getReceiptNo());
        wrapper.eq(query.getOrderId() != null, PurReceipt::getOrderId, query.getOrderId());
        wrapper.eq(query.getSupplierId() != null, PurReceipt::getSupplierId, query.getSupplierId());
        wrapper.eq(query.getStatus() != null, PurReceipt::getStatus, query.getStatus());
        wrapper.orderByDesc(PurReceipt::getId);

        IPage<PurReceipt> result = receiptMapper.selectPage(page, wrapper);
        Map<Long, MdmSupplier> suppliers = loadSuppliers(result.getRecords().stream()
                .map(PurReceipt::getSupplierId).toList());
        Map<Long, MdmWarehouse> warehouses = new HashMap<>();
        for (Long whId : result.getRecords().stream().map(PurReceipt::getWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet())) {
            MdmWarehouse warehouse = mdmApi.getWarehouseById(whId);
            if (warehouse != null) {
                warehouses.put(whId, warehouse);
            }
        }

        List<PurReceiptVo> rows = result.getRecords().stream().map(receipt -> {
            PurReceiptVo vo = new PurReceiptVo();
            vo.setId(receipt.getId());
            vo.setReceiptNo(receipt.getReceiptNo());
            vo.setOrderId(receipt.getOrderId());
            vo.setOrderNo(receipt.getOrderNo());
            vo.setSupplierId(receipt.getSupplierId());
            vo.setWarehouseId(receipt.getWarehouseId());
            vo.setReceiptDate(receipt.getReceiptDate());
            vo.setStatus(receipt.getStatus());
            vo.setInvOrderId(receipt.getInvOrderId());
            vo.setInvOrderNo(receipt.getInvOrderNo());
            vo.setRemark(receipt.getRemark());
            vo.setCreateBy(receipt.getCreateBy());
            vo.setCreateTime(receipt.getCreateTime());
            MdmSupplier supplier = suppliers.get(receipt.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getSupplierName());
            }
            MdmWarehouse warehouse = warehouses.get(receipt.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getWarehouseName());
            }
            return vo;
        }).toList();
        return PageResult.of(result, rows);
    }

    @Override
    public PurReceipt selectById(Long id) {
        PurReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            return null;
        }
        List<PurReceiptDetail> details = receiptDetailMapper.selectList(
                new LambdaQueryWrapper<PurReceiptDetail>()
                        .eq(PurReceiptDetail::getReceiptId, id)
                        .orderByAsc(PurReceiptDetail::getId));
        Set<Long> materialIds = details.stream().map(PurReceiptDetail::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MdmMaterial> materials = materialIds.isEmpty() ? new HashMap<>()
                : mdmApi.listMaterialsByIds(materialIds).stream()
                .collect(Collectors.toMap(MdmMaterial::getId, m -> m, (a, b) -> a));
        for (PurReceiptDetail detail : details) {
            MdmMaterial material = materials.get(detail.getMaterialId());
            if (material != null) {
                detail.setMaterialName(material.getMaterialName());
            }
        }
        receipt.setDetails(details);
        return receipt;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(PurReceiptDto dto) {
        PurOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new ServiceException("来源采购订单不存在");
        }
        if (!PurOrder.STATUS_AUDITED.equals(order.getStatus())
                && !PurOrder.STATUS_PARTIAL.equals(order.getStatus())) {
            throw new ServiceException("只有已审核未收完的订单才能收货");
        }
        if (dto.getWarehouseId() == null || mdmApi.getWarehouseById(dto.getWarehouseId()) == null) {
            throw new ServiceException("收货仓库不存在");
        }

        PurReceipt receipt = new PurReceipt();
        receipt.setReceiptNo(noGenerator.nextReceiptNo());
        receipt.setOrderId(order.getId());
        receipt.setOrderNo(order.getOrderNo());
        receipt.setSupplierId(order.getSupplierId());
        receipt.setWarehouseId(dto.getWarehouseId());
        receipt.setReceiptDate(dto.getReceiptDate());
        receipt.setStatus(PurReceipt.STATUS_DRAFT);
        receipt.setRemark(dto.getRemark());
        receiptMapper.insert(receipt);

        for (PurReceiptDetailDto item : dto.getDetails()) {
            PurOrderDetail orderDetail = orderDetailMapper.selectById(item.getOrderDetailId());
            if (orderDetail == null || !orderDetail.getOrderId().equals(order.getId())) {
                throw new ServiceException("来源订单明细不存在：id=" + item.getOrderDetailId());
            }
            PurReceiptDetail detail = new PurReceiptDetail();
            detail.setReceiptId(receipt.getId());
            detail.setOrderDetailId(orderDetail.getId());
            // 物料以订单明细为准，不接受前端传，避免收货物料与订单对不上
            detail.setMaterialId(orderDetail.getMaterialId());
            detail.setQty(item.getQty());
            detail.setRemark(item.getRemark());
            receiptDetailMapper.insert(detail);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (PurReceipt receipt : receiptMapper.selectByIds(ids)) {
            if (!PurReceipt.STATUS_DRAFT.equals(receipt.getStatus())) {
                throw new ServiceException("收货单 " + receipt.getReceiptNo() + " 不是草稿，不能删除");
            }
        }
        receiptMapper.deleteByIds(ids);
        for (Long id : ids) {
            receiptDetailMapper.delete(new LambdaQueryWrapper<PurReceiptDetail>()
                    .eq(PurReceiptDetail::getReceiptId, id));
        }
    }

    /**
     * 确认收货 —— 跨模块业务闭环。
     * <p>
     * 涉及三张表 + 库存模块的台账/流水表，全部在<b>一个本地事务</b>里完成：
     * 采购模块与库存模块运行在同一进程、同一数据源，Spring 的本地事务能保证
     * 一旦中途失败（比如库存不足、反写失败）全部一起回滚，不会出现「收货了但没入库」
     * 或「入库了但订单没反写」。这正是选择模块化单体而不是微服务的实际收益 ——
     * 拆成微服务后这里就必须引入分布式事务或对账补偿。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirm(Long id) {
        PurReceipt receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw new ServiceException("收货单不存在或已删除");
        }
        if (!PurReceipt.STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new ServiceException("收货单已确认，不能重复确认");
        }
        PurOrder order = orderMapper.selectById(receipt.getOrderId());
        if (order == null) {
            throw new ServiceException("来源采购订单不存在");
        }
        if (!PurOrder.STATUS_AUDITED.equals(order.getStatus())
                && !PurOrder.STATUS_PARTIAL.equals(order.getStatus())) {
            throw new ServiceException("采购订单状态不允许收货");
        }

        // 先抢占状态（草稿 → 已确认）再干活。
        //
        // 上面那句 "status == 草稿" 不是原子的：并发确认两次（用户双击，或两个人同时点）
        // 会双双通过检查，结果生成两张入库单、库存被加两次。
        // 这里用带条件的 UPDATE 让数据库行锁来互斥，只有第一个请求能改成功。
        // 放在最前面是为了让失败方在动任何数据之前就退出；
        // 若后续步骤出错，整个事务回滚，状态自然退回草稿。
        PurReceipt claimed = new PurReceipt();
        claimed.setStatus(PurReceipt.STATUS_CONFIRMED);
        int claimedRows = receiptMapper.update(claimed, new LambdaUpdateWrapper<PurReceipt>()
                .eq(PurReceipt::getId, id)
                .eq(PurReceipt::getStatus, PurReceipt.STATUS_DRAFT));
        if (claimedRows == 0) {
            throw new ServiceException("收货单已被确认，请刷新后重试");
        }

        List<PurReceiptDetail> details = receiptDetailMapper.selectList(
                new LambdaQueryWrapper<PurReceiptDetail>()
                        .eq(PurReceiptDetail::getReceiptId, id)
                        .orderByAsc(PurReceiptDetail::getId));
        if (details.isEmpty()) {
            throw new ServiceException("收货单没有明细，不能确认");
        }

        List<InboundItem> inboundItems = new ArrayList<>();
        String operator = SecurityUtils.getUsernameOrSystem();
        for (PurReceiptDetail detail : details) {
            PurOrderDetail orderDetail = orderDetailMapper.selectById(detail.getOrderDetailId());
            if (orderDetail == null || !orderDetail.getOrderId().equals(order.getId())) {
                throw new ServiceException("来源订单明细不存在：id=" + detail.getOrderDetailId());
            }
            // 先查一次给出明确的提示信息；真正防超收靠下面带条件的原子更新
            BigDecimal received = orderDetail.getReceivedQty() == null ? BigDecimal.ZERO : orderDetail.getReceivedQty();
            BigDecimal unreceived = orderDetail.getQty().subtract(received);
            if (detail.getQty().compareTo(unreceived) > 0) {
                throw new ServiceException("本次收货数量超过订单未收数量，剩余可收：" + unreceived);
            }
            int rows = orderDetailMapper.increaseReceivedQty(orderDetail.getId(), detail.getQty(), operator);
            if (rows == 0) {
                // 并发下另一张收货单抢先收走，条件不再满足
                throw new ServiceException("本次收货数量超过订单未收数量，请刷新后重试");
            }
            InboundItem item = new InboundItem();
            item.setMaterialId(detail.getMaterialId());
            item.setQty(detail.getQty());
            item.setPrice(orderDetail.getPrice());
            item.setRemark(detail.getRemark());
            inboundItems.add(item);
        }

        // 反写订单状态：所有明细都收满 → 已完成，否则部分收货
        refreshOrderStatus(order.getId());

        // 调库存模块生成入库单（含台账与流水），purchase 不碰 inv_* 表
        InboundRequest request = new InboundRequest();
        request.setWarehouseId(receipt.getWarehouseId());
        request.setBizDate(receipt.getReceiptDate());
        request.setSourceType(SOURCE_PUR_RECEIPT);
        request.setSourceNo(receipt.getReceiptNo());
        request.setRemark("采购收货 " + receipt.getReceiptNo());
        request.setItems(inboundItems);
        InvOrder invOrder = stockService.createInboundFromReceipt(request);

        // 回写生成的入库单 id/no（状态在方法开头已经抢占过了，这里不必再带状态条件：
        // 走到这一行说明当前事务已经独占该收货单，别的请求进不来）
        PurReceipt update = new PurReceipt();
        update.setId(receipt.getId());
        update.setStatus(PurReceipt.STATUS_CONFIRMED);
        update.setInvOrderId(invOrder.getId());
        update.setInvOrderNo(invOrder.getOrderNo());
        receiptMapper.updateById(update);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 依据明细的已收/应收数量刷新订单状态：3已完成 / 2部分收货 */
    private void refreshOrderStatus(Long orderId) {
        PurOrder order = orderMapper.selectById(orderId);
        if (order == null || !PurOrder.STATUS_AUDITED.equals(order.getStatus())
                && !PurOrder.STATUS_PARTIAL.equals(order.getStatus())) {
            return;
        }
        List<PurOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<PurOrderDetail>()
                .eq(PurOrderDetail::getOrderId, orderId));
        boolean allReceived = !details.isEmpty();
        for (PurOrderDetail detail : details) {
            BigDecimal received = detail.getReceivedQty() == null ? BigDecimal.ZERO : detail.getReceivedQty();
            if (received.compareTo(detail.getQty()) < 0) {
                allReceived = false;
                break;
            }
        }
        PurOrder update = new PurOrder();
        update.setId(orderId);
        update.setStatus(allReceived ? PurOrder.STATUS_FINISHED : PurOrder.STATUS_PARTIAL);
        orderMapper.updateById(update);
    }

    private Map<Long, MdmSupplier> loadSuppliers(List<Long> supplierIds) {
        Set<Long> ids = supplierIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return mdmApi.listSuppliersByIds(ids).stream()
                .collect(Collectors.toMap(MdmSupplier::getId, s -> s, (a, b) -> a));
    }
}

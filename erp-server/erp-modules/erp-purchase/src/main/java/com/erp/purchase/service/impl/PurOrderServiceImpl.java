package com.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.framework.security.SecurityUtils;
import com.erp.mdm.api.MdmApi;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.purchase.domain.PurOrder;
import com.erp.purchase.domain.PurOrderDetail;
import com.erp.purchase.domain.dto.PurOrderDetailDto;
import com.erp.purchase.domain.dto.PurOrderDto;
import com.erp.purchase.domain.dto.PurOrderQuery;
import com.erp.purchase.domain.vo.PurOrderVo;
import com.erp.purchase.mapper.PurOrderDetailMapper;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.service.IPurOrderService;
import com.erp.purchase.service.PurNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采购订单服务实现。
 */
@Service
@RequiredArgsConstructor
public class PurOrderServiceImpl implements IPurOrderService {

    private final PurOrderMapper orderMapper;
    private final PurOrderDetailMapper orderDetailMapper;
    private final PurNoGenerator noGenerator;
    private final MdmApi mdmApi;

    @Override
    public PageResult<PurOrderVo> pageList(PurOrderQuery query) {
        Page<PurOrder> page = query.toPage();
        LambdaQueryWrapper<PurOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getOrderNo() != null, PurOrder::getOrderNo, query.getOrderNo());
        wrapper.eq(query.getSupplierId() != null, PurOrder::getSupplierId, query.getSupplierId());
        wrapper.eq(query.getStatus() != null, PurOrder::getStatus, query.getStatus());
        wrapper.orderByDesc(PurOrder::getId);

        IPage<PurOrder> result = orderMapper.selectPage(page, wrapper);
        Map<Long, MdmSupplier> suppliers = loadSuppliers(result.getRecords().stream()
                .map(PurOrder::getSupplierId).toList());
        List<PurOrderVo> rows = result.getRecords().stream().map(order -> {
            PurOrderVo vo = new PurOrderVo();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setSupplierId(order.getSupplierId());
            vo.setOrderDate(order.getOrderDate());
            vo.setExpectDate(order.getExpectDate());
            vo.setStatus(order.getStatus());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setRemark(order.getRemark());
            vo.setAuditBy(order.getAuditBy());
            vo.setAuditTime(order.getAuditTime());
            vo.setCreateBy(order.getCreateBy());
            vo.setCreateTime(order.getCreateTime());
            MdmSupplier supplier = suppliers.get(order.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getSupplierName());
            }
            return vo;
        }).toList();
        return PageResult.of(result, rows);
    }

    @Override
    public PurOrder selectById(Long id) {
        PurOrder order = orderMapper.selectById(id);
        if (order == null) {
            return null;
        }
        order.setDetails(loadDetails(id));
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(PurOrderDto dto) {
        checkSupplier(dto.getSupplierId());
        checkDetails(dto.getDetails());

        PurOrder order = new PurOrder();
        order.setOrderNo(noGenerator.nextOrderNo());
        order.setSupplierId(dto.getSupplierId());
        order.setOrderDate(dto.getOrderDate());
        order.setExpectDate(dto.getExpectDate());
        order.setStatus(PurOrder.STATUS_DRAFT);
        order.setTotalAmount(sumAmount(dto.getDetails()));
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        saveDetails(order.getId(), dto.getDetails());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(PurOrderDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("订单ID不能为空");
        }
        PurOrder exist = orderMapper.selectById(dto.getId());
        if (exist == null) {
            throw new ServiceException("订单不存在或已删除");
        }
        if (!PurOrder.STATUS_DRAFT.equals(exist.getStatus())) {
            throw new ServiceException("只有草稿状态的订单才能修改");
        }
        checkSupplier(dto.getSupplierId());
        checkDetails(dto.getDetails());

        PurOrder order = new PurOrder();
        order.setId(dto.getId());
        order.setSupplierId(dto.getSupplierId());
        order.setOrderDate(dto.getOrderDate());
        order.setExpectDate(dto.getExpectDate());
        // 总金额重新按明细汇总，前端传什么都不看
        order.setTotalAmount(sumAmount(dto.getDetails()));
        order.setRemark(dto.getRemark());
        orderMapper.updateById(order);

        orderDetailMapper.delete(new LambdaQueryWrapper<PurOrderDetail>()
                .eq(PurOrderDetail::getOrderId, dto.getId()));
        saveDetails(dto.getId(), dto.getDetails());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (PurOrder order : orderMapper.selectByIds(ids)) {
            if (!PurOrder.STATUS_DRAFT.equals(order.getStatus())) {
                throw new ServiceException("订单 " + order.getOrderNo() + " 不是草稿，不能删除");
            }
        }
        orderMapper.deleteByIds(ids);
        for (Long id : ids) {
            orderDetailMapper.delete(new LambdaQueryWrapper<PurOrderDetail>()
                    .eq(PurOrderDetail::getOrderId, id));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void audit(Long id) {
        PurOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在或已删除");
        }
        if (!PurOrder.STATUS_DRAFT.equals(order.getStatus())) {
            throw new ServiceException("订单已审核，不能重复审核");
        }
        Long count = orderDetailMapper.selectCount(new LambdaQueryWrapper<PurOrderDetail>()
                .eq(PurOrderDetail::getOrderId, id));
        if (count == null || count == 0) {
            throw new ServiceException("订单没有明细，不能审核");
        }
        // 带条件的状态流转：把「检查状态」与「改状态」合并成一条 SQL，
        // 由数据库行锁保证并发下只有一个人成功（详细理由见
        // com.erp.inventory.service.impl.InvOrderServiceImpl#transitStatus 的注释）
        PurOrder update = new PurOrder();
        update.setStatus(PurOrder.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getUsernameOrSystem());
        update.setAuditTime(LocalDateTime.now());
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<PurOrder>()
                .eq(PurOrder::getId, id)
                .eq(PurOrder::getStatus, PurOrder.STATUS_DRAFT));
        if (rows == 0) {
            throw new ServiceException("订单状态已变更（可能已被他人审核），请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(Long id) {
        PurOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在或已删除");
        }
        // 只有草稿或已审核（尚未开始收货）的订单能作废；
        // 已部分/全部收货的订单涉及已入库的库存，作废会让账实不符，必须走退货流程而非作废
        if (!PurOrder.STATUS_DRAFT.equals(order.getStatus())
                && !PurOrder.STATUS_AUDITED.equals(order.getStatus())) {
            throw new ServiceException("订单已收货或有单据流转，不能作废");
        }
        PurOrder update = new PurOrder();
        update.setStatus(PurOrder.STATUS_VOID);
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<PurOrder>()
                .eq(PurOrder::getId, id)
                .in(PurOrder::getStatus, PurOrder.STATUS_DRAFT, PurOrder.STATUS_AUDITED));
        if (rows == 0) {
            throw new ServiceException("订单状态已变更，请刷新后重试");
        }
    }

    @Override
    public List<PurOrder> listPendingOrders() {
        List<PurOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurOrder>()
                .in(PurOrder::getStatus, PurOrder.STATUS_AUDITED, PurOrder.STATUS_PARTIAL)
                .orderByDesc(PurOrder::getId));
        List<PurOrder> pending = new ArrayList<>();
        for (PurOrder order : orders) {
            List<PurOrderDetail> details = loadDetails(order.getId());
            boolean hasPending = details.stream()
                    .anyMatch(d -> d.getUnreceivedQty() != null && d.getUnreceivedQty().signum() > 0);
            if (hasPending) {
                order.setDetails(details);
                pending.add(order);
            }
        }
        return pending;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 读明细并填充物料名与未收数量 */
    private List<PurOrderDetail> loadDetails(Long orderId) {
        List<PurOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<PurOrderDetail>()
                .eq(PurOrderDetail::getOrderId, orderId)
                .orderByAsc(PurOrderDetail::getId));
        Set<Long> materialIds = details.stream().map(PurOrderDetail::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MdmMaterial> materials = materialIds.isEmpty() ? new HashMap<>()
                : mdmApi.listMaterialsByIds(materialIds).stream()
                .collect(Collectors.toMap(MdmMaterial::getId, m -> m, (a, b) -> a));
        for (PurOrderDetail detail : details) {
            MdmMaterial material = materials.get(detail.getMaterialId());
            if (material != null) {
                detail.setMaterialName(material.getMaterialName());
            }
            BigDecimal received = detail.getReceivedQty() == null ? BigDecimal.ZERO : detail.getReceivedQty();
            detail.setUnreceivedQty(detail.getQty().subtract(received));
        }
        return details;
    }

    private void saveDetails(Long orderId, List<PurOrderDetailDto> details) {
        for (PurOrderDetailDto item : details) {
            PurOrderDetail detail = new PurOrderDetail();
            detail.setOrderId(orderId);
            detail.setMaterialId(item.getMaterialId());
            detail.setQty(item.getQty());
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            detail.setPrice(price);
            detail.setAmount(price.multiply(item.getQty()));
            detail.setReceivedQty(BigDecimal.ZERO);
            detail.setRemark(item.getRemark());
            orderDetailMapper.insert(detail);
        }
    }

    private BigDecimal sumAmount(List<PurOrderDetailDto> details) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurOrderDetailDto item : details) {
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            total = total.add(price.multiply(item.getQty()));
        }
        return total;
    }

    private void checkSupplier(Long supplierId) {
        if (supplierId == null || mdmApi.getSupplierById(supplierId) == null) {
            throw new ServiceException("供应商不存在");
        }
    }

    private void checkDetails(List<PurOrderDetailDto> details) {
        if (details == null || details.isEmpty()) {
            throw new ServiceException("订单明细不能为空");
        }
        for (PurOrderDetailDto item : details) {
            if (item.getMaterialId() == null || !mdmApi.existsMaterial(item.getMaterialId())) {
                throw new ServiceException("物料不存在：id=" + item.getMaterialId());
            }
            if (item.getQty() == null || item.getQty().signum() <= 0) {
                throw new ServiceException("明细数量必须大于 0");
            }
        }
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

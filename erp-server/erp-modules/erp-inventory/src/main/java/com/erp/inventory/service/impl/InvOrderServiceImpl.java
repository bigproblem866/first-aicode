package com.erp.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.framework.security.SecurityUtils;
import com.erp.inventory.constant.InvConstants;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.domain.InvOrderDetail;
import com.erp.inventory.domain.dto.InvOrderDetailDto;
import com.erp.inventory.domain.dto.InvOrderDto;
import com.erp.inventory.domain.dto.InvOrderQuery;
import com.erp.inventory.domain.vo.InvOrderVo;
import com.erp.inventory.mapper.InvOrderDetailMapper;
import com.erp.inventory.mapper.InvOrderMapper;
import com.erp.inventory.service.IInvOrderService;
import com.erp.inventory.service.IInvStockService;
import com.erp.inventory.service.InvOrderNoGenerator;
import com.erp.mdm.api.MdmApi;
import com.erp.mdm.domain.MdmWarehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 出入库单服务实现。
 * <p>
 * 台账的增减不自己写 SQL，而是调用 {@link IInvStockService} —— 保证「改台账必写流水」
 * 这条不变量只在一处实现。
 */
@Service
@RequiredArgsConstructor
public class InvOrderServiceImpl implements IInvOrderService {

    private final InvOrderMapper orderMapper;
    private final InvOrderDetailMapper orderDetailMapper;
    private final InvOrderNoGenerator orderNoGenerator;
    private final IInvStockService stockService;
    private final MdmApi mdmApi;

    @Override
    public PageResult<InvOrderVo> pageList(InvOrderQuery query) {
        Page<InvOrder> page = query.toPage();
        LambdaQueryWrapper<InvOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getOrderNo() != null, InvOrder::getOrderNo, query.getOrderNo());
        wrapper.eq(query.getOrderType() != null, InvOrder::getOrderType, query.getOrderType());
        wrapper.eq(query.getStatus() != null, InvOrder::getStatus, query.getStatus());
        wrapper.eq(query.getWarehouseId() != null, InvOrder::getWarehouseId, query.getWarehouseId());
        wrapper.orderByDesc(InvOrder::getId);

        IPage<InvOrder> result = orderMapper.selectPage(page, wrapper);
        Map<Long, MdmWarehouse> warehouses = new HashMap<>();
        for (Long id : result.getRecords().stream().map(InvOrder::getWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet())) {
            MdmWarehouse warehouse = mdmApi.getWarehouseById(id);
            if (warehouse != null) {
                warehouses.put(id, warehouse);
            }
        }
        List<InvOrderVo> rows = result.getRecords().stream().map(order -> {
            InvOrderVo vo = new InvOrderVo();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setOrderType(order.getOrderType());
            vo.setWarehouseId(order.getWarehouseId());
            vo.setBizDate(order.getBizDate());
            vo.setStatus(order.getStatus());
            vo.setSourceType(order.getSourceType());
            vo.setSourceNo(order.getSourceNo());
            vo.setRemark(order.getRemark());
            vo.setAuditBy(order.getAuditBy());
            vo.setAuditTime(order.getAuditTime());
            vo.setCreateBy(order.getCreateBy());
            vo.setCreateTime(order.getCreateTime());
            MdmWarehouse warehouse = warehouses.get(order.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getWarehouseName());
            }
            return vo;
        }).toList();

        return PageResult.of(result, rows);
    }

    @Override
    public InvOrder selectById(Long id) {
        InvOrder order = orderMapper.selectById(id);
        if (order == null) {
            return null;
        }
        order.setDetails(orderDetailMapper.selectList(new LambdaQueryWrapper<InvOrderDetail>()
                .eq(InvOrderDetail::getOrderId, id)
                .orderByAsc(InvOrderDetail::getId)));
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(InvOrderDto dto) {
        checkOrderType(dto.getOrderType());
        checkWarehouse(dto.getWarehouseId());
        checkDetails(dto.getDetails());

        InvOrder order = new InvOrder();
        order.setOrderNo(orderNoGenerator.next(prefixOf(dto.getOrderType())));
        order.setOrderType(dto.getOrderType());
        order.setWarehouseId(dto.getWarehouseId());
        order.setBizDate(dto.getBizDate());
        order.setStatus(InvOrder.STATUS_DRAFT);
        order.setSourceType(InvOrder.SOURCE_MANUAL);
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        saveDetails(order.getId(), dto.getDetails());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(InvOrderDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("单据ID不能为空");
        }
        InvOrder exist = orderMapper.selectById(dto.getId());
        if (exist == null) {
            throw new ServiceException("单据不存在或已删除");
        }
        if (!InvOrder.STATUS_DRAFT.equals(exist.getStatus())) {
            throw new ServiceException("只有草稿状态的单据才能修改");
        }
        checkOrderType(dto.getOrderType());
        checkWarehouse(dto.getWarehouseId());
        checkDetails(dto.getDetails());

        InvOrder order = new InvOrder();
        order.setId(dto.getId());
        order.setOrderType(dto.getOrderType());
        order.setWarehouseId(dto.getWarehouseId());
        order.setBizDate(dto.getBizDate());
        order.setRemark(dto.getRemark());
        orderMapper.updateById(order);

        // 先删旧明细再插新明细：逻辑删除，历史行仍可追溯到旧版本
        orderDetailMapper.delete(new LambdaQueryWrapper<InvOrderDetail>()
                .eq(InvOrderDetail::getOrderId, dto.getId()));
        saveDetails(dto.getId(), dto.getDetails());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<InvOrder> orders = orderMapper.selectByIds(ids);
        for (InvOrder order : orders) {
            if (!InvOrder.STATUS_DRAFT.equals(order.getStatus())) {
                throw new ServiceException("单据 " + order.getOrderNo() + " 不是草稿，不能删除");
            }
        }
        orderMapper.deleteByIds(ids);
        for (Long id : ids) {
            orderDetailMapper.delete(new LambdaQueryWrapper<InvOrderDetail>()
                    .eq(InvOrderDetail::getOrderId, id));
        }
    }

    /**
     * 审核：逐条明细更新库存台账，并各写一条流水；整单状态置为已审核。
     * <p>
     * 入库走 UPSERT，出库走带条件的原子扣减（库存不足时 {@link IInvStockService#decreaseStock}
     * 会抛异常，整个事务回滚）。方法整体在事务中，保证「台账、流水、单据状态」三者同生共死。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void audit(Long id) {
        InvOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("单据不存在或已删除");
        }
        if (!InvOrder.STATUS_DRAFT.equals(order.getStatus())) {
            throw new ServiceException("单据已审核，不能重复审核");
        }
        List<InvOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<InvOrderDetail>()
                .eq(InvOrderDetail::getOrderId, id)
                .orderByAsc(InvOrderDetail::getId));
        if (details.isEmpty()) {
            throw new ServiceException("单据没有明细，不能审核");
        }

        // 先把状态原子改掉，再动库存。顺序不能反。
        //
        // 前面那句 "status == 草稿" 的检查不是原子的：两个并发请求（用户双击、或两个人同时点）
        // 会双双读到草稿、双双通过检查。真正的互斥靠这里这条带条件的 UPDATE：
        // 只有第一个请求能改成功，第二个影响 0 行、抛异常退出，库存就只会加一次。
        // 若反过来先动库存再改状态，两次库存变动都已经发生，再抛异常也来不及了。
        transitStatus(order.getId(), InvOrder.STATUS_DRAFT, InvOrder.STATUS_AUDITED);

        boolean inbound = InvOrder.TYPE_IN.equals(order.getOrderType());
        for (InvOrderDetail detail : details) {
            if (inbound) {
                stockService.increaseStock(order.getWarehouseId(), detail.getMaterialId(), detail.getQty(),
                        InvConstants.BIZ_IN, InvConstants.SOURCE_INV_ORDER,
                        order.getId(), order.getOrderNo(), order.getBizDate(), order.getRemark());
            } else {
                stockService.decreaseStock(order.getWarehouseId(), detail.getMaterialId(), detail.getQty(),
                        InvConstants.BIZ_OUT, InvConstants.SOURCE_INV_ORDER,
                        order.getId(), order.getOrderNo(), order.getBizDate(), order.getRemark());
            }
        }
    }

    /**
     * 作废：只有已审核的单据可以作废。反向冲回库存并各写一条反向流水，
     * <b>不删除原流水</b>（流水表只增不改，这是对账的前提）。
     * <p>
     * 反向方向与业务类型的关系：入库单作废 = 一次出库（biz_type=OUT，qty 为负），
     * 出库单作废 = 一次入库（biz_type=IN，qty 为正）。
     * 这样「流水 qty 为正 ⟺ biz_type=IN」这条全局约定始终成立。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(Long id) {
        InvOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("单据不存在或已删除");
        }
        if (!InvOrder.STATUS_AUDITED.equals(order.getStatus())) {
            throw new ServiceException("只有已审核的单据才能作废");
        }
        List<InvOrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<InvOrderDetail>()
                .eq(InvOrderDetail::getOrderId, id)
                .orderByAsc(InvOrderDetail::getId));

        // 同样先原子改状态再冲回库存，理由见 audit 方法的注释
        transitStatus(order.getId(), InvOrder.STATUS_AUDITED, InvOrder.STATUS_VOID);

        boolean inbound = InvOrder.TYPE_IN.equals(order.getOrderType());
        for (InvOrderDetail detail : details) {
            if (inbound) {
                // 原入库 → 冲回是减少库存
                stockService.decreaseStock(order.getWarehouseId(), detail.getMaterialId(), detail.getQty(),
                        InvConstants.BIZ_OUT, InvConstants.SOURCE_INV_ORDER,
                        order.getId(), order.getOrderNo(), order.getBizDate(), "作废冲回：" + order.getOrderNo());
            } else {
                // 原出库 → 冲回是增加库存
                stockService.increaseStock(order.getWarehouseId(), detail.getMaterialId(), detail.getQty(),
                        InvConstants.BIZ_IN, InvConstants.SOURCE_INV_ORDER,
                        order.getId(), order.getOrderNo(), order.getBizDate(), "作废冲回：" + order.getOrderNo());
            }
        }
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private void saveDetails(Long orderId, List<InvOrderDetailDto> details) {
        for (InvOrderDetailDto item : details) {
            InvOrderDetail detail = new InvOrderDetail();
            detail.setOrderId(orderId);
            detail.setMaterialId(item.getMaterialId());
            detail.setQty(item.getQty());
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            detail.setPrice(price);
            // 金额一律后端算，前端传什么都不看
            detail.setAmount(price.multiply(item.getQty()));
            detail.setRemark(item.getRemark());
            orderDetailMapper.insert(detail);
        }
    }

    /**
     * 带条件的状态流转 —— 并发安全的关键。
     * <p>
     * 与 {@code updateById} 的区别就在那个 {@code eq(status, fromStatus)} 条件：
     * 它把「检查当前状态」和「改状态」合并成一条 SQL，由数据库在行锁内保证原子性。
     * 影响行数为 0 说明状态已被别人改过，直接抛异常让整个事务回滚。
     * <p>
     * 这正是「先判断再修改」在并发下不可靠的标准解法：
     * JVM 里的 {@code if (order.getStatus() == DRAFT)} 与随后的 UPDATE 之间，
     * 别的线程随时可能插进来改掉状态。
     *
     * @param fromStatus 期望的当前状态；不匹配则不更新
     * @param toStatus   目标状态
     */
    private void transitStatus(Long orderId, String fromStatus, String toStatus) {
        InvOrder update = new InvOrder();
        update.setStatus(toStatus);
        if (InvOrder.STATUS_AUDITED.equals(toStatus)) {
            update.setAuditBy(SecurityUtils.getUsernameOrSystem());
            update.setAuditTime(LocalDateTime.now());
        }
        // 传 entity 而不是 null，是为了让 MetaObjectHandler 自动填充 update_by / update_time
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<InvOrder>()
                .eq(InvOrder::getId, orderId)
                .eq(InvOrder::getStatus, fromStatus));
        if (rows == 0) {
            throw new ServiceException("单据状态已变更（可能已被他人审核或作废），请刷新后重试");
        }
    }

    private void checkOrderType(String orderType) {
        if (!InvOrder.TYPE_IN.equals(orderType) && !InvOrder.TYPE_OUT.equals(orderType)) {
            throw new ServiceException("单据类型只能是 IN 或 OUT");
        }
    }

    private String prefixOf(String orderType) {
        return InvOrder.TYPE_IN.equals(orderType) ? InvConstants.PREFIX_IN : InvConstants.PREFIX_OUT;
    }

    private void checkWarehouse(Long warehouseId) {
        if (warehouseId == null || mdmApi.getWarehouseById(warehouseId) == null) {
            throw new ServiceException("仓库不存在");
        }
    }

    private void checkDetails(List<InvOrderDetailDto> details) {
        if (details == null || details.isEmpty()) {
            throw new ServiceException("单据明细不能为空");
        }
        for (InvOrderDetailDto item : details) {
            if (item.getMaterialId() == null || !mdmApi.existsMaterial(item.getMaterialId())) {
                throw new ServiceException("物料不存在：id=" + item.getMaterialId());
            }
            if (item.getQty() == null || item.getQty().signum() <= 0) {
                throw new ServiceException("明细数量必须大于 0");
            }
        }
    }
}

package com.erp.purchase.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.purchase.domain.PurOrder;
import com.erp.purchase.domain.PurReceipt;
import com.erp.purchase.domain.dto.PurReceiptDto;
import com.erp.purchase.domain.dto.PurReceiptQuery;
import com.erp.purchase.domain.vo.PurReceiptVo;
import com.erp.purchase.service.IPurOrderService;
import com.erp.purchase.service.IPurReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 采购收货接口。
 */
@Tag(name = "采购-收货", description = "收货单增删、确认收货（生成入库单并增加库存）")
@RestController
@RequestMapping("/api/pur/receipt")
@RequiredArgsConstructor
public class PurReceiptController {

    private final IPurReceiptService receiptService;
    private final IPurOrderService orderService;

    @Operation(summary = "收货单分页列表")
    @PreAuthorize("@ss.hasPermi('pur:receipt:list')")
    @GetMapping("/list")
    public R<PageResult<PurReceiptVo>> list(PurReceiptQuery query) {
        return R.ok(receiptService.pageList(query));
    }

    @Operation(summary = "待收货订单", description = "已审核且未收完的订单（含明细、已收/未收数量），收货选单用")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/pendingOrders")
    public R<List<PurOrder>> pendingOrders() {
        return R.ok(orderService.listPendingOrders());
    }

    @Operation(summary = "收货单详情（含明细）")
    @PreAuthorize("@ss.hasPermi('pur:receipt:query')")
    @GetMapping("/{id}")
    public R<PurReceipt> getInfo(@PathVariable Long id) {
        return R.ok(receiptService.selectById(id));
    }

    @Operation(summary = "新增收货单（草稿）")
    @PreAuthorize("@ss.hasPermi('pur:receipt:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody PurReceiptDto dto) {
        receiptService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "删除收货单（仅草稿）")
    @PreAuthorize("@ss.hasPermi('pur:receipt:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        receiptService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }

    @Operation(summary = "确认收货",
            description = "反写订单已收数量 → 判断订单状态 → 生成入库单 → 加库存、写流水，全部在同一事务内")
    @PreAuthorize("@ss.hasPermi('pur:receipt:confirm')")
    @PostMapping("/confirm/{id}")
    public R<Void> confirm(@PathVariable Long id) {
        receiptService.confirm(id);
        return R.ok();
    }
}

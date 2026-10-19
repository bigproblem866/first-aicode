package com.erp.purchase.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.purchase.domain.PurOrder;
import com.erp.purchase.domain.dto.PurOrderDto;
import com.erp.purchase.domain.dto.PurOrderQuery;
import com.erp.purchase.domain.vo.PurOrderVo;
import com.erp.purchase.service.IPurOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 采购订单接口。
 */
@Tag(name = "采购-订单", description = "采购订单增删改查、审核、作废")
@RestController
@RequestMapping("/api/pur/order")
@RequiredArgsConstructor
public class PurOrderController {

    private final IPurOrderService orderService;

    @Operation(summary = "订单分页列表")
    @PreAuthorize("@ss.hasPermi('pur:order:list')")
    @GetMapping("/list")
    public R<PageResult<PurOrderVo>> list(PurOrderQuery query) {
        return R.ok(orderService.pageList(query));
    }

    @Operation(summary = "订单详情（含明细）")
    @PreAuthorize("@ss.hasPermi('pur:order:query')")
    @GetMapping("/{id}")
    public R<PurOrder> getInfo(@PathVariable Long id) {
        return R.ok(orderService.selectById(id));
    }

    @Operation(summary = "新增订单（含明细）", description = "总金额由明细汇总，不接受前端传入")
    @PreAuthorize("@ss.hasPermi('pur:order:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody PurOrderDto dto) {
        orderService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改订单（仅草稿）")
    @PreAuthorize("@ss.hasPermi('pur:order:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody PurOrderDto dto) {
        orderService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除订单（仅草稿）")
    @PreAuthorize("@ss.hasPermi('pur:order:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        orderService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }

    @Operation(summary = "审核订单")
    @PreAuthorize("@ss.hasPermi('pur:order:audit')")
    @PostMapping("/audit/{id}")
    public R<Void> audit(@PathVariable Long id) {
        orderService.audit(id);
        return R.ok();
    }

    @Operation(summary = "作废订单")
    @PreAuthorize("@ss.hasPermi('pur:order:audit')")
    @PostMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }
}

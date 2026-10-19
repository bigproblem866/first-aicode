package com.erp.inventory.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.domain.dto.InvOrderDto;
import com.erp.inventory.domain.dto.InvOrderQuery;
import com.erp.inventory.domain.vo.InvOrderVo;
import com.erp.inventory.service.IInvOrderService;
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
 * 出入库单接口。入库单/出库单共用本组接口，靠 orderType=IN/OUT 区分。
 */
@Tag(name = "库存-出入库单", description = "单据增删改查、审核、作废")
@RestController
@RequestMapping("/api/inv/order")
@RequiredArgsConstructor
public class InvOrderController {

    private final IInvOrderService orderService;

    @Operation(summary = "单据分页列表")
    @PreAuthorize("@ss.hasPermi('inv:order:list')")
    @GetMapping("/list")
    public R<PageResult<InvOrderVo>> list(InvOrderQuery query) {
        return R.ok(orderService.pageList(query));
    }

    @Operation(summary = "单据详情（含明细）")
    @PreAuthorize("@ss.hasPermi('inv:order:query')")
    @GetMapping("/{id}")
    public R<InvOrder> getInfo(@PathVariable Long id) {
        return R.ok(orderService.selectById(id));
    }

    @Operation(summary = "新增单据（含明细）")
    @PreAuthorize("@ss.hasPermi('inv:order:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody InvOrderDto dto) {
        orderService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改单据（仅草稿）")
    @PreAuthorize("@ss.hasPermi('inv:order:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody InvOrderDto dto) {
        orderService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除单据（仅草稿）")
    @PreAuthorize("@ss.hasPermi('inv:order:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        orderService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }

    @Operation(summary = "审核单据", description = "写库存台账 + 写流水；出库用带条件的原子扣减防超卖")
    @PreAuthorize("@ss.hasPermi('inv:order:audit')")
    @PostMapping("/audit/{id}")
    public R<Void> audit(@PathVariable Long id) {
        orderService.audit(id);
        return R.ok();
    }

    @Operation(summary = "作废单据", description = "仅已审核可作废，反向冲回库存并写反向流水")
    @PreAuthorize("@ss.hasPermi('inv:order:audit')")
    @PostMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }
}

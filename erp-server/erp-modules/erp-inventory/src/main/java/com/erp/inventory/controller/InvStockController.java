package com.erp.inventory.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.inventory.domain.dto.InvStockQuery;
import com.erp.inventory.domain.vo.InvStockVo;
import com.erp.inventory.service.IInvStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存台账接口。
 */
@Tag(name = "库存-台账", description = "库存查询")
@RestController
@RequestMapping("/api/inv/stock")
@RequiredArgsConstructor
public class InvStockController {

    private final IInvStockService stockService;

    @Operation(summary = "库存台账分页", description = "物料/仓库名称由 MdmApi 填充，不 join mdm 表")
    @PreAuthorize("@ss.hasPermi('inv:stock:list')")
    @GetMapping("/list")
    public R<PageResult<InvStockVo>> list(InvStockQuery query) {
        return R.ok(stockService.pageList(query));
    }
}

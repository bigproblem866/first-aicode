package com.erp.inventory.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.inventory.domain.dto.InvStockLogQuery;
import com.erp.inventory.domain.vo.InvStockLogVo;
import com.erp.inventory.service.IInvStockLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存流水接口。
 */
@Tag(name = "库存-流水", description = "库存流水分页查询")
@RestController
@RequestMapping("/api/inv/log")
@RequiredArgsConstructor
public class InvStockLogController {

    private final IInvStockLogService stockLogService;

    @Operation(summary = "库存流水分页")
    @PreAuthorize("@ss.hasPermi('inv:log:list')")
    @GetMapping("/list")
    public R<PageResult<InvStockLogVo>> list(InvStockLogQuery query) {
        return R.ok(stockLogService.pageList(query));
    }
}

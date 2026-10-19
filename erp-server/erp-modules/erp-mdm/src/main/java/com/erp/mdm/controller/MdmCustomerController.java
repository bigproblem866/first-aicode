package com.erp.mdm.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.mdm.domain.MdmCustomer;
import com.erp.mdm.domain.dto.MdmCustomerDto;
import com.erp.mdm.domain.dto.MdmCustomerQuery;
import com.erp.mdm.service.IMdmCustomerService;
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
import java.util.List;

/**
 * 客户管理接口。
 */
@Tag(name = "主数据-客户", description = "客户的增删改查")
@RestController
@RequestMapping("/api/mdm/customer")
@RequiredArgsConstructor
public class MdmCustomerController {

    private final IMdmCustomerService customerService;

    @Operation(summary = "客户分页列表")
    @PreAuthorize("@ss.hasPermi('mdm:customer:list')")
    @GetMapping("/list")
    public R<PageResult<MdmCustomer>> list(MdmCustomerQuery query) {
        return R.ok(customerService.pageList(query));
    }

    @Operation(summary = "客户下拉")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/optionselect")
    public R<List<MdmCustomer>> optionselect() {
        return R.ok(customerService.listOptions());
    }

    @Operation(summary = "客户详情")
    @PreAuthorize("@ss.hasPermi('mdm:customer:query')")
    @GetMapping("/{id}")
    public R<MdmCustomer> getInfo(@PathVariable Long id) {
        return R.ok(customerService.selectById(id));
    }

    @Operation(summary = "新增客户")
    @PreAuthorize("@ss.hasPermi('mdm:customer:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody MdmCustomerDto dto) {
        customerService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改客户")
    @PreAuthorize("@ss.hasPermi('mdm:customer:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody MdmCustomerDto dto) {
        customerService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除客户")
    @PreAuthorize("@ss.hasPermi('mdm:customer:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        customerService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }
}

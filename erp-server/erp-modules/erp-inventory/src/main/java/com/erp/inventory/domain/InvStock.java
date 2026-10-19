package com.erp.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 库存台账实体，对应表 inv_stock。一个仓库 + 一个物料一行。
 * <p>
 * 本表只存「现在有多少」，会被反复更新；每次变动的前后数量记在 {@link InvStockLog}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stock")
public class InvStock extends BaseEntity {

    /** 仓库ID（mdm_warehouse.id） */
    private Long warehouseId;

    /** 物料ID（mdm_material.id） */
    private Long materialId;

    /** 现存数量 */
    private BigDecimal qty;

    /** 备注 */
    private String remark;
}

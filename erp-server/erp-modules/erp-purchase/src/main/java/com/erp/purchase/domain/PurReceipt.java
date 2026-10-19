package com.erp.purchase.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购收货单实体，对应表 pur_receipt。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_receipt")
public class PurReceipt extends BaseEntity {

    /** 状态：0草稿 */
    public static final String STATUS_DRAFT = "0";
    /** 状态：1已确认（已入库） */
    public static final String STATUS_CONFIRMED = "1";
    /** 状态：2已作废 */
    public static final String STATUS_VOID = "2";

    /** 收货单号，如 PR20261016001 */
    private String receiptNo;

    /** 来源采购订单ID */
    private Long orderId;

    /** 来源采购订单号（冗余字段，列表页不用再联表） */
    private String orderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 收货仓库ID */
    private Long warehouseId;

    /** 收货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiptDate;

    /** 状态：0草稿 1已确认（已入库） 2已作废 */
    private String status;

    /** 生成的入库单ID */
    private Long invOrderId;

    /** 生成的入库单号 */
    private String invOrderNo;

    /** 备注 */
    private String remark;

    /** 明细行，非数据库字段 */
    @TableField(exist = false)
    private List<PurReceiptDetail> details = new ArrayList<>();
}

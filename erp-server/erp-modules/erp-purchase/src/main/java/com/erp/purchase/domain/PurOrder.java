package com.erp.purchase.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单实体，对应表 pur_order。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_order")
public class PurOrder extends BaseEntity {

    /** 状态：0草稿 */
    public static final String STATUS_DRAFT = "0";
    /** 状态：1已审核 */
    public static final String STATUS_AUDITED = "1";
    /** 状态：2部分收货 */
    public static final String STATUS_PARTIAL = "2";
    /** 状态：3已完成 */
    public static final String STATUS_FINISHED = "3";
    /** 状态：4已作废 */
    public static final String STATUS_VOID = "4";

    /** 订单号，如 PO20261016001 */
    private String orderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 下单日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    /** 期望到货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectDate;

    /** 状态：0草稿 1已审核 2部分收货 3已完成 4已作废 */
    private String status;

    /** 订单总金额（由明细汇总，不接受前端传入） */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    /** 审核人 */
    private String auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 明细行，非数据库字段 */
    @TableField(exist = false)
    private List<PurOrderDetail> details = new ArrayList<>();
}

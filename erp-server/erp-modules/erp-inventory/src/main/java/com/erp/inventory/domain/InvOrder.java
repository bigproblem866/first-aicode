package com.erp.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 出入库单实体，对应表 inv_order。入库单与出库单共用一张表，用 orderType 区分。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_order")
public class InvOrder extends BaseEntity {

    public static final String TYPE_IN = "IN";
    public static final String TYPE_OUT = "OUT";

    /** 状态：0草稿 1已审核 2已作废 */
    public static final String STATUS_DRAFT = "0";
    public static final String STATUS_AUDITED = "1";
    public static final String STATUS_VOID = "2";

    /** 来源类型：手工录入 */
    public static final String SOURCE_MANUAL = "MANUAL";
    /** 来源类型：采购收货生成 */
    public static final String SOURCE_PUR_RECEIPT = "PUR_RECEIPT";

    /** 单据号，如 IN20261016001 */
    private String orderNo;

    /** 单据类型：IN入库 OUT出库 */
    private String orderType;

    /** 仓库ID */
    private Long warehouseId;

    /** 业务日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bizDate;

    /** 状态：0草稿 1已审核 2已作废 */
    private String status;

    /** 来源单据类型：MANUAL手工录入 PUR_RECEIPT采购收货 */
    private String sourceType;

    /** 来源单据号 */
    private String sourceNo;

    /** 备注 */
    private String remark;

    /** 审核人 */
    private String auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 明细行，非数据库字段 */
    @TableField(exist = false)
    private List<InvOrderDetail> details = new ArrayList<>();
}

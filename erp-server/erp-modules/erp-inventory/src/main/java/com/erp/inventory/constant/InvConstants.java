package com.erp.inventory.constant;

/**
 * 库存模块内部常量。只在本模块使用的值放这里，不往 common 里堆。
 */
public final class InvConstants {

    private InvConstants() {
    }

    // ---------------- 业务类型（inv_stock_log.biz_type）----------------
    /** 入库 */
    public static final String BIZ_IN = "IN";
    /** 出库 */
    public static final String BIZ_OUT = "OUT";
    /** 盘点调整 */
    public static final String BIZ_ADJUST = "ADJUST";

    // ---------------- 来源单据类型（inv_stock_log.source_type）----------------
    /** 来源为出入库单 */
    public static final String SOURCE_INV_ORDER = "INV_ORDER";
    /** 来源为采购收货 */
    public static final String SOURCE_PUR_RECEIPT = "PUR_RECEIPT";

    // ---------------- 单据号前缀 ----------------
    public static final String PREFIX_IN = "IN";
    public static final String PREFIX_OUT = "OUT";
}

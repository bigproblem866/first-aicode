package com.erp.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.purchase.domain.PurOrder;
import com.erp.purchase.domain.PurReceipt;
import com.erp.purchase.mapper.PurOrderMapper;
import com.erp.purchase.mapper.PurReceiptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 采购单据号生成器：{前缀}{yyyyMMdd}{三位流水}，如 PO20261016001 / PR20261016001。
 * <p>
 * 订单号与收货单号分别从各自的表取当天最大流水 —— 不能共用一个表查，
 * 否则收货单号永远从 001 开始、当天第二张就撞唯一索引。
 * <p>
 * 单号都有唯一索引兜底：真并发撞了就插入失败，不会产生重复单号。
 */
@Component
@RequiredArgsConstructor
public class PurNoGenerator {

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 采购订单号前缀 */
    public static final String PREFIX_PO = "PO";
    /** 采购收货单号前缀 */
    public static final String PREFIX_PR = "PR";

    private final PurOrderMapper orderMapper;
    private final PurReceiptMapper receiptMapper;

    public String nextOrderNo() {
        String head = PREFIX_PO + LocalDate.now().format(DATE_PATTERN);
        PurOrder last = orderMapper.selectOne(new LambdaQueryWrapper<PurOrder>()
                .likeRight(PurOrder::getOrderNo, head)
                .orderByDesc(PurOrder::getOrderNo)
                .last("LIMIT 1"));
        int seq = nextSeq(head, last == null ? null : last.getOrderNo());
        return head + String.format("%03d", seq);
    }

    public String nextReceiptNo() {
        String head = PREFIX_PR + LocalDate.now().format(DATE_PATTERN);
        PurReceipt last = receiptMapper.selectOne(new LambdaQueryWrapper<PurReceipt>()
                .likeRight(PurReceipt::getReceiptNo, head)
                .orderByDesc(PurReceipt::getReceiptNo)
                .last("LIMIT 1"));
        int seq = nextSeq(head, last == null ? null : last.getReceiptNo());
        return head + String.format("%03d", seq);
    }

    private int nextSeq(String head, String lastNo) {
        if (lastNo == null || lastNo.length() <= head.length()) {
            return 1;
        }
        try {
            return Integer.parseInt(lastNo.substring(head.length())) + 1;
        } catch (NumberFormatException ignored) {
            // 单号被人为改乱时不阻断业务；唯一索引会拦住真正的重复
            return 1;
        }
    }
}

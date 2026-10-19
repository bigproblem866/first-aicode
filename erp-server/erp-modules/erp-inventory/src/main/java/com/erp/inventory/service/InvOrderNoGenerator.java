package com.erp.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.mapper.InvOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 出入库单号生成器：{前缀}{yyyyMMdd}{三位流水}，如 IN20261016001。
 * <p>
 * 说明：这里取「当天已有单号的最大流水 + 1」，在真正高并发的场景应改为序列表/Redis，
 * 本工程是单体、单库，且 order_no 有唯一索引兜底，冲突时插入直接失败，不会产生重复单号。
 */
@Component
@RequiredArgsConstructor
public class InvOrderNoGenerator {

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InvOrderMapper orderMapper;

    public String next(String prefix) {
        String datePart = LocalDate.now().format(DATE_PATTERN);
        String head = prefix + datePart;
        InvOrder last = orderMapper.selectOne(new LambdaQueryWrapper<InvOrder>()
                .likeRight(InvOrder::getOrderNo, head)
                .orderByDesc(InvOrder::getOrderNo)
                .last("LIMIT 1"));
        int seq = 1;
        if (last != null && last.getOrderNo() != null && last.getOrderNo().length() > head.length()) {
            try {
                seq = Integer.parseInt(last.getOrderNo().substring(head.length())) + 1;
            } catch (NumberFormatException ignored) {
                // 单号被人为改乱时不阻断业务，从 1 开始；唯一索引会拦住真正的重复
                seq = 1;
            }
        }
        return head + String.format("%03d", seq);
    }
}

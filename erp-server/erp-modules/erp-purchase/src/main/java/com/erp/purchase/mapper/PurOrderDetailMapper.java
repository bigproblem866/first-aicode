package com.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.purchase.domain.PurOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 采购订单明细 Mapper。
 */
@Mapper
public interface PurOrderDetailMapper extends BaseMapper<PurOrderDetail> {

    /**
     * 累加已收货数量，<b>带条件保证不会超收</b>：
     * {@code qty - received_qty >= #{qty}} 由数据库在行锁内判断，影响行数为 0 即超收。
     * <p>
     * 与库存扣减同理：不能写成「先查未收数量 → Java 判断 → update」，
     * 并发下两张收货单会各自认为"没超"，一起把 received_qty 累加超。
     */
    @Update("""
            UPDATE pur_order_detail
            SET received_qty = received_qty + #{qty},
                update_by    = #{operator},
                update_time  = NOW()
            WHERE id = #{id}
              AND is_deleted = 0
              AND qty - received_qty >= #{qty}
            """)
    int increaseReceivedQty(@Param("id") Long id,
                            @Param("qty") BigDecimal qty,
                            @Param("operator") String operator);
}

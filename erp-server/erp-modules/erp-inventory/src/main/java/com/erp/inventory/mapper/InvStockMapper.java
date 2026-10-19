package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.domain.InvStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 库存台账 Mapper。
 * <p>
 * 这里两个方法是整个库存模块<b>并发安全的关键</b>，必须写成一条 SQL、
 * 由数据库的行锁保证原子性，不能用「先查再改」：
 * <ul>
 *   <li>{@link #increaseQty} 入库用 UPSERT，台账行不存在时自动创建；</li>
 *   <li>{@link #decreaseQty} 出库用「带 qty &gt;= 扣减量 条件」的原子更新，
 *       靠影响行数判断是否够扣。</li>
 * </ul>
 */
@Mapper
public interface InvStockMapper extends BaseMapper<InvStock> {

    /**
     * 入库：UPSERT 台账。
     * <p>
     * 首次入库时没有台账行，靠 INSERT 自动创建；
     * 唯一键 uk_inv_stock_wh_material(warehouse_id, material_id) 触发冲突时改走 UPDATE 累加。
     * 返回影响行数（1=新增，2=更新，MySQL 语义），此处只关心成功与否。
     */
    int increaseQty(@Param("warehouseId") Long warehouseId,
                    @Param("materialId") Long materialId,
                    @Param("qty") BigDecimal qty,
                    @Param("operator") String operator);

    /**
     * 出库：<b>带条件的原子扣减</b>。
     * <p>
     * {@code WHERE ... AND qty >= #{qty}} 由数据库一次性完成「判断 + 扣减」，
     * 并发下第二个请求会因为行锁串行执行、看到扣减后的 qty 而条件不满足。
     * 返回 0 行 = 库存不足，调用方据此抛异常回滚。
     * <p>
     * 绝不能拆成「select 查 qty → Java 判断 → update 扣减」：并发下两个请求
     * 都会查到"够扣"，然后一起扣成负数。
     */
    int decreaseQty(@Param("warehouseId") Long warehouseId,
                    @Param("materialId") Long materialId,
                    @Param("qty") BigDecimal qty,
                    @Param("operator") String operator);
}

package com.agri.traceability.mapper;

import com.agri.traceability.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 原子扣减库存
     * 通过数据库行锁保证并发安全，防止超卖
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 受影响行数（0表示库存不足或商品不存在）
     */
    @Update("UPDATE product SET stock = stock - #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND status = 1")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 原子恢复库存（取消订单时使用）
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 受影响行数
     */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}

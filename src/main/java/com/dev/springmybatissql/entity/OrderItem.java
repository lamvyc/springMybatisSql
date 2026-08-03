package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体
 *
 * 对应表：order_item
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    /** 成交单价 */
    private BigDecimal price;
}
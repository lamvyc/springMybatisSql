package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 *
 * 对应表：orders（order 是关键字，故用复数 orders）
 */
@Data
@TableName("`orders`")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 下单用户ID */
    private Long userId;

    /** 订单号 */
    private String orderNo;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 状态：0待支付 1已支付 2已发货 3已完成 4已取消 */
    private Integer status;

    /** 下单时间 */
    private LocalDateTime createTime;
}
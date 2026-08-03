package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 *
 * 对应表：product
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 分类ID */
    private Long categoryId;

    /** 单价 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 状态：1上架 0下架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
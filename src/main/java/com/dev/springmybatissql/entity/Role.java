package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体
 *
 * 对应表：role
 */
@Data
@TableName("role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    private String name;
}
package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * 对应表：user（user 是 MySQL 关键字，MyBatis Plus 的 @TableName
 * 会自动加反引号处理，原生 SQL 中需要手写 `user`）
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createTime;
}
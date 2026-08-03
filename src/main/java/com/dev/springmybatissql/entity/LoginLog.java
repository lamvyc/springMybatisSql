package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户登录日志实体
 *
 * 对应表：login_log
 * 用于"连续登录"分析题
 */
@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 登录日期 */
    private LocalDate loginDate;
}
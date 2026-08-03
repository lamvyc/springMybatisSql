package com.dev.springmybatissql.vo;

import com.dev.springmybatissql.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户模块：MyBatis VS MyBatis Plus 对比结果 VO
 *
 * 用途：同一业务功能分别用两套 Mapper 实现后，
 * 返回结果 + 代码量对比，帮助理解差异。
 */
@Data
@Builder
public class UserCompareVO {

    /** 原生 MyBatis 分页结果 */
    private List<User> mybatisList;

    /** MyBatis Plus 分页结果 */
    private List<User> plusList;

    /** 原生 MyBatis 实现的方法数 */
    private int mybatisMethodCount;

    /** MyBatis Plus 实现的方法数（BaseMapper自带，为0） */
    private int plusMethodCount;

    /** 原生日志说明 */
    private String message;
}
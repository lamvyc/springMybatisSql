package com.dev.springmybatissql.service;

import com.dev.springmybatissql.dto.UserDTO;
import com.dev.springmybatissql.entity.User;
import com.dev.springmybatissql.vo.UserCompareVO;

import java.util.List;

/**
 * 用户服务接口（阶段7：MyBatis VS MyBatis Plus 对比）
 */
public interface UserService {

    // ==================== MyBatis Plus 实现（简单CRUD，零SQL） ====================

    /** 新增用户（Plus） */
    User plusCreate(UserDTO dto);

    /** 根据ID查询（Plus） */
    User plusGetById(Long id);

    /** 修改用户（Plus） */
    void plusUpdate(Long id, UserDTO dto);

    /** 删除用户（Plus） */
    void plusDelete(Long id);

    /** 用户名模糊查询（Plus，LambdaQueryWrapper） */
    List<User> plusSearch(String username);

    // ==================== 原生 MyBatis 实现（手写XML SQL） ====================

    /** 新增用户（Native） */
    User nativeCreate(UserDTO dto);

    /** 根据ID查询（Native） */
    User nativeGetById(Long id);

    /** 修改用户（Native） */
    void nativeUpdate(Long id, UserDTO dto);

    /** 删除用户（Native） */
    void nativeDelete(Long id);

    /** 用户名模糊查询（Native） */
    List<User> nativeSearch(String username);

    // ==================== 对比 ====================

    /** 同一分页功能双实现对比 */
    UserCompareVO compare(int page, int size);
}
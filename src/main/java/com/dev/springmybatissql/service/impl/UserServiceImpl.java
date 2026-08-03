package com.dev.springmybatissql.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dev.springmybatissql.dto.UserDTO;
import com.dev.springmybatissql.entity.User;
import com.dev.springmybatissql.mapper.UserNativeMapper;
import com.dev.springmybatissql.mapper.UserPlusMapper;
import com.dev.springmybatissql.service.UserService;
import com.dev.springmybatissql.vo.UserCompareVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现（阶段7核心：同一业务两种实现方式对比）
 *
 * - plusXxx：MyBatis Plus 实现。继承 BaseMapper，CRUD 零 SQL。
 * - nativeXxx：原生 MyBatis 实现。SQL 手写在 UserNativeMapper.xml。
 *
 * 【对比结论】
 * 1. 代码量：Plus 版每个方法1-3行，Native 版需要手写XML + 接口方法
 * 2. SQL控制：Plus 自动生成，Native 完全可控
 * 3. 适用场景：简单CRUD用Plus；复杂查询/报表用Native
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserPlusMapper userPlusMapper;

    @Autowired
    private UserNativeMapper userNativeMapper;

    // ==================== MyBatis Plus 实现 ====================

    @Override
    public User plusCreate(UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        // BaseMapper 自动生成 INSERT，主键自增回填
        userPlusMapper.insert(user);
        return user;
    }

    @Override
    public User plusGetById(Long id) {
        return userPlusMapper.selectById(id);
    }

    @Override
    public void plusUpdate(Long id, UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setId(id);
        // 只更新非空字段（MyBatis Plus 默认策略）
        userPlusMapper.updateById(user);
    }

    @Override
    public void plusDelete(Long id) {
        userPlusMapper.deleteById(id);
    }

    @Override
    public List<User> plusSearch(String username) {
        // LambdaQueryWrapper：类型安全的条件构造器，避免魔法字符串
        return userPlusMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getUsername, username)
                .orderByAsc(User::getId));
    }

    // ==================== 原生 MyBatis 实现 ====================

    @Override
    public User nativeCreate(UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        // 手写 INSERT，useGeneratedKeys 回填id
        userNativeMapper.insert(user);
        return user;
    }

    @Override
    public User nativeGetById(Long id) {
        return userNativeMapper.selectById(id);
    }

    @Override
    public void nativeUpdate(Long id, UserDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setId(id);
        userNativeMapper.updateById(user);
    }

    @Override
    public void nativeDelete(Long id) {
        userNativeMapper.deleteById(id);
    }

    @Override
    public List<User> nativeSearch(String username) {
        return userNativeMapper.selectByName(username);
    }

    // ==================== 对比 ====================

    @Override
    public UserCompareVO compare(int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 5;
        }

        // MyBatis Plus 分页：借助分页插件 + selectPage，无需手写 limit
        Page<User> plusPage = userPlusMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByAsc(User::getId));

        // 原生 MyBatis 分页：手动计算 offset，手写 LIMIT #{offset}, #{size}
        int offset = (page - 1) * size;
        List<User> nativeList = userNativeMapper.selectByPage(offset, size);

        return UserCompareVO.builder()
                .mybatisList(nativeList)
                .plusList(plusPage.getRecords())
                .mybatisMethodCount(countNativeMethods())
                .plusMethodCount(0)
                .message("原生MyBatis需手写SQL和XML映射；MyBatis Plus继承BaseMapper零SQL自动CRUD")
                .build();
    }

    /**
     * 原生 MyBatis 实现的接口方法数（统计演示用途）
     */
    private int countNativeMethods() {
        return UserNativeMapper.class.getDeclaredMethods().length;
    }
}
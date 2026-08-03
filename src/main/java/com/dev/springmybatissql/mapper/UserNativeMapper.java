package com.dev.springmybatissql.mapper;

import com.dev.springmybatissql.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper（原生 MyBatis 方式）
 *
 * 对比解读：所有 CRUD SQL 都需在 UserNativeMapper.xml 中手写。
 * 语义清晰、可控性强，但代码量大。
 */
public interface UserNativeMapper {

    /**
     * 新增用户（写操作，返回影响行数）
     */
    int insert(User user);

    /**
     * 根据ID查询
     */
    User selectById(@Param("id") Long id);

    /**
     * 修改用户
     */
    int updateById(User user);

    /**
     * 根据ID删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 按用户名模糊查询
     */
    List<User> selectByName(@Param("username") String username);

    /**
     * 分页查询（limit/offset）
     */
    List<User> selectByPage(@Param("offset") int offset, @Param("size") int size);

    /**
     * 总数
     */
    long countAll();
}
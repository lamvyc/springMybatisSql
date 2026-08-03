package com.dev.springmybatissql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dev.springmybatissql.entity.User;

/**
 * 用户 Mapper（MyBatis Plus 方式）
 *
 * 对比解读：
 * - UserNativeMapper（原生 MyBatis）：需要在 XML 中手写 insert/select/update/delete 全套 SQL
 * - UserPlusMapper（MyBatis Plus）：继承 BaseMapper 后，直接获得
 *   insert / deleteById / selectById / updateById / selectList 等通用 CRUD，
 *   无需编写任何 SQL！
 *
 * 结论：简单 CRUD 用 MyBatis Plus 效率高（零 SQL）；
 *       复杂查询、报表统计仍需原生 MyBatis 手写 SQL。
 */
public interface UserPlusMapper extends BaseMapper<User> {
}
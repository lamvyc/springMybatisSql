package com.dev.springmybatissql.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 *
 * 作用：扫描 Mapper 接口所在包，将接口动态注册为 Spring Bean。
 * 启动后框架会为每个 Mapper 接口生成代理对象，注入 Service 层使用。
 *
 * 说明：
 * - MyBatis Plus 底层就是 MyBatis，因此同样通过 @MapperScan 扫描。
 * - 也可以在每个 Mapper 接口上加 @Mapper 注解（二选一即可）。
 */
@Configuration
@MapperScan("com.dev.springmybatissql.mapper")
public class MybatisConfig {
}
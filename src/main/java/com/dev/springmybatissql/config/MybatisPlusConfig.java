package com.dev.springmybatissql.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置类
 *
 * 作用：注册 MyBatis Plus 插件。
 * 这里注册了分页插件，用于阶段7 的 MyBatis Plus 分页查询。
 *
 * 提示：阶段1~6 使用原生 MyBatis 分页（手动 limit ? offset ?），
 * 只有阶段7 的 BaseMapper.selectPage 需要本插件。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis Plus 拦截器（插件机制）
     * 分页插件：对接入的 SQL 自动拼接 limit 语法
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大 500 条，防止恶意分页请求
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}

package com.dev.springmybatissql.common;

import lombok.Data;

/**
 * 统一 API 响应结构
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> {

    /** 业务状态码：200 成功，其他失败 */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 数据 */
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
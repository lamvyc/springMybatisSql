package com.dev.springmybatissql.common;

import lombok.Data;

/**
 * 统一 API 响应结构
 *
 * @param <T> 数据类型
 */
@Data // Lombok：自动生成 get/set/toString/equals/hashCode（无需手写）
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

/**
 * class Result<T>
 *             │
 *             ├── data 是 T
 *             ├── getData() 返回 T
 *             └── setData(T)
 *
 *                     ↑
 *
 * public static <T> Result<T> success(T data)
 *               │        │          │
 *               │        │          └── 参数类型
 *               │        └──────────── 返回值类型
 *               └──────────────────── 声明泛型 T（<T> = 声明一个类型变量；后面的所有 T 都是在使用这个类型变量。）
 * <p>
 * Java 泛型分为类级别泛型（包括 class、interface、record）和方法级别泛型。
 * 类级别泛型在类型声明时定义，整个类（或接口、record）都可以直接使用；
 * 方法由于没有独立的泛型声明位置，因此 Java 规定：如果方法需要拥有自己的泛型，必须在返回值前使用 <T>（或 <E> 等）先声明类型变量，
 * 其作用范围仅限于当前方法。泛型名称只是类型变量名，T、E、K、V 等都可以使用，只是遵循 Java 的命名约定。
 * <p>
 *
 *  <T> 可以不用 T 吗？完全可以。Java 社区有约定俗成的命名：
 * | 名称  | 含义          | 常见场景     |
 * | --- | ----------- | -------- |
 * | `T` | Type（类型）    | 最常用      |
 * | `E` | Element（元素） | List、Set |
 * | `K` | Key（键）      | Map      |
 * | `V` | Value（值）    | Map      |
 * | `R` | Return（返回值） | 函数式接口    |
 * | `U` | 第二个 Type    | 多个泛型参数   |
 * <p>
 *
 * 拓展：类型擦除、泛型边界（extends）、通配符（?）【后续再看】
 * <p>
 * */


/**
 * 【编译期】—— 一切定义的时刻
 *    编写代码：public class Result<T> { ... }
 *    ↓
 *    编译器读取完整代码，理解 Result 类的全部结构
 *    ↓
 *    生成 .class 字节码文件（类定义已经固化下来）
 *    ↓
 *    ✅ 此时类已经"存在"了（作为一段固定的字节码）
 *
 *    ════════════════════════════════════════════
 *
 * 【运行期】—— 实际执行的时刻
 *    JVM 加载 Result.class 到内存
 *    ↓
 *    有人调用 Result.success(data)
 *    ↓
 *    执行到 new Result<>() 这一行
 *    ↓
 *    JVM 根据已加载的类定义，在堆内存中创建一个实例
 *    ↓
 *    ✅ 因为类定义早已存在，所以创建实例没有任何障碍
 *
 *
 *         <p>真正的原因是"定义先于使用"</p>
 * 语言	            能否内部 new 自己	     原因
 * Java	                ✅	        编译期完成类定义
 * Python	            ✅	        运行期先执行 class 定义，后调用方法
 * JavaScript	        ✅	        同 Python，定义先于调用
 * Go	                ✅	        编译期完成类型定义
 * C++	                ✅	        同 Java
 *
 *
 * */
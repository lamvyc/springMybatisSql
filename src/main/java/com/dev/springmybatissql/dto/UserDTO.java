package com.dev.springmybatissql.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增/修改 DTO（参数校验）
 *
 * 学习点：DTO 用于接收前端请求参数，与数据库实体 User 解耦。
 * 新增场景不接收 id/createTime（由数据库自动生成）。
 */
@Data
public class UserDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名最长50字符")
    private String username;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号 */
    private String phone;

    /** 状态：1启用 0禁用 */
    private Integer status;
}
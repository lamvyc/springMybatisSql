package com.dev.springmybatissql.controller;

import com.dev.springmybatissql.common.Result;
import com.dev.springmybatissql.dto.UserDTO;
import com.dev.springmybatissql.entity.User;
import com.dev.springmybatissql.service.UserService;
import com.dev.springmybatissql.vo.UserCompareVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 Controller（阶段7：MyBatis VS MyBatis Plus 对比）
 *
 * 相同业务提供两套端点：
 * - /api/users/plus/xxx  ：MyBatis Plus 实现（零SQL）
 * - /api/users/native/xxx：原生 MyBatis 实现（手写XML）
 * - /api/users/compare    ：分页双实现对比
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ==================== MyBatis Plus 端点 ====================

    /** 新增用户（Plus） */
    @PostMapping("/plus")
    public Result<User> plusCreate(@Valid @RequestBody UserDTO dto) {
        return Result.ok(userService.plusCreate(dto));
    }

    /** 根据ID查询（Plus） */
    @GetMapping("/plus/{id}")
    public Result<User> plusGet(@PathVariable Long id) {
        return Result.ok(userService.plusGetById(id));
    }

    /** 修改用户（Plus） */
    @PutMapping("/plus/{id}")
    public Result<Void> plusUpdate(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.plusUpdate(id, dto);
        return Result.ok();
    }

    /** 删除用户（Plus） */
    @DeleteMapping("/plus/{id}")
    public Result<Void> plusDelete(@PathVariable Long id) {
        userService.plusDelete(id);
        return Result.ok();
    }

    /** 用户名模糊查询（Plus） */
    @GetMapping("/plus/search")
    public Result<List<User>> plusSearch(@RequestParam String username) {
        return Result.ok(userService.plusSearch(username));
    }

    // ==================== 原生 MyBatis 端点 ====================

    /** 新增用户（Native） */
    @PostMapping("/native")
    public Result<User> nativeCreate(@Valid @RequestBody UserDTO dto) {
        return Result.ok(userService.nativeCreate(dto));
    }

    /** 根据ID查询（Native） */
    @GetMapping("/native/{id}")
    public Result<User> nativeGet(@PathVariable Long id) {
        return Result.ok(userService.nativeGetById(id));
    }

    /** 修改用户（Native） */
    @PutMapping("/native/{id}")
    public Result<Void> nativeUpdate(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.nativeUpdate(id, dto);
        return Result.ok();
    }

    /** 删除用户（Native） */
    @DeleteMapping("/native/{id}")
    public Result<Void> nativeDelete(@PathVariable Long id) {
        userService.nativeDelete(id);
        return Result.ok();
    }

    /** 用户名模糊查询（Native） */
    @GetMapping("/native/search")
    public Result<List<User>> nativeSearch(@RequestParam String username) {
        return Result.ok(userService.nativeSearch(username));
    }

    // ==================== 对比 ====================

    /** 分页功能：原生 MyBatis VS MyBatis Plus 对比 */
    @GetMapping("/compare")
    public Result<UserCompareVO> compare(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "5") int size) {
        return Result.ok(userService.compare(page, size));
    }
}

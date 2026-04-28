package com.library.user.controller;

import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.user.dto.UserDTO.*;
import com.library.user.entity.User;
import com.library.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserResponse>> listUsers() {
        List<User> users = userService.listAll();
        List<UserResponse> responses = users.stream()
                .map(userService::toUserResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "根据ID获取用户")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public Result<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(userService.toUserResponse(user));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        User user = userService.create(request);
        return Result.success("创建成功", userService.toUserResponse(user));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public Result<UserResponse> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateRequest request) {
        User user = userService.update(id, request);
        return Result.success("更新成功", userService.toUserResponse(user));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }
}

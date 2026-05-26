package com.library.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class UserDTO {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
        private String password;

        @NotBlank(message = "真实姓名不能为空")
        private String realName;

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        private String email;

        private String gender;
    }

    @Data
    public static class UpdateRequest {
        private String realName;
        private String phone;
        private String email;
        private String gender;
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    @Data
    public static class UserResponse {
        private Long userId;
        private String username;
        private String realName;
        private String userType;
        private String gender;
        private String phone;
        private String email;
        private String status;
        private String lastLoginTime;
        private String createdAt;
    }

    @Data
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserResponse user;
    }
}

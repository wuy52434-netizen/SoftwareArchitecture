package com.library.user.service;

import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.user.dto.UserDTO.LoginRequest;
import com.library.user.dto.UserDTO.LoginResponse;
import com.library.user.dto.UserDTO.RegisterRequest;
import com.library.user.dto.UserDTO.UserResponse;
import com.library.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试
 * 技术栈: JUnit5 + Mockito + AssertJ
 * 覆盖登录、注册、Token 刷新的业务规则与异常分支。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User buildUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("zhangsan");
        user.setPassword("$2a$10$encodedHash");
        user.setUserType("reader");
        user.setStatus("active");
        return user;
    }

    private UserResponse buildUserResponse() {
        UserResponse res = new UserResponse();
        res.setUserId(1L);
        res.setUsername("zhangsan");
        res.setUserType("reader");
        res.setStatus("active");
        res.setMaxBorrowCount(10);
        res.setCurrentBorrowCount(0);
        return res;
    }

    private LoginRequest buildLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setUsername("zhangsan");
        req.setPassword("123456");
        return req;
    }

    @Nested
    @DisplayName("login")
    class LoginTest {

        @Test
        @DisplayName("用户不存在时抛 USER_NOT_FOUND")
        void userNotFound_shouldThrow() {
            when(userService.getByUsername("zhangsan")).thenReturn(null);

            assertThatThrownBy(() -> authService.login(buildLoginRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
                    });
            verify(userService, never()).updateLastLoginTime(anyLong());
        }

        @Test
        @DisplayName("密码错误时抛 USER_PASSWORD_ERROR")
        void wrongPassword_shouldThrow() {
            User user = buildUser();
            when(userService.getByUsername("zhangsan")).thenReturn(user);
            when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(buildLoginRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(ResultCode.USER_PASSWORD_ERROR.getCode());
                    });
            verify(userService, never()).updateLastLoginTime(anyLong());
        }

        @Test
        @DisplayName("账号被禁用时抛 USER_DISABLED")
        void disabledUser_shouldThrow() {
            User user = buildUser();
            user.setStatus("inactive");
            when(userService.getByUsername("zhangsan")).thenReturn(user);
            when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(buildLoginRequest()))
                    .isInstanceOf(BusinessException.class);
            verify(userService, never()).updateLastLoginTime(anyLong());
        }

        @Test
        @DisplayName("登录成功返回 accessToken + refreshToken 并更新最后登录时间")
        void loginOk_shouldReturnTokens() {
            User user = buildUser();
            when(userService.getByUsername("zhangsan")).thenReturn(user);
            when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);
            when(userService.toUserResponse(user)).thenReturn(buildUserResponse());

            LoginResponse resp = authService.login(buildLoginRequest());

            assertThat(resp.getAccessToken()).isNotBlank();
            assertThat(resp.getRefreshToken()).isNotBlank();
            assertThat(resp.getTokenType()).isEqualTo("Bearer");
            assertThat(resp.getExpiresIn()).isEqualTo(7200L);
            assertThat(resp.getUser().getUsername()).isEqualTo("zhangsan");

            verify(userService).updateLastLoginTime(1L);
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTest {

        private RegisterRequest buildRegisterRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("lisi");
            req.setPassword("123456");
            req.setRealName("李四");
            return req;
        }

        @Test
        @DisplayName("注册成功返回 token")
        void registerOk() {
            User newUser = buildUser();
            newUser.setUsername("lisi");
            when(userService.create(any(RegisterRequest.class))).thenReturn(newUser);
            when(userService.toUserResponse(newUser)).thenReturn(buildUserResponse());

            LoginResponse resp = authService.register(buildRegisterRequest());

            assertThat(resp.getAccessToken()).isNotBlank();
            verify(userService).create(any(RegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTest {

        @Test
        @DisplayName("无效 RefreshToken 抛 UNAUTHORIZED")
        void invalidRefreshToken_shouldThrow() {
            // 传入不合法 token 由 JwtUtil 拒绝
            assertThatThrownBy(() -> authService.refreshToken("invalid-token"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class CurrentUserTest {

        @Test
        @DisplayName("根据 userId 返回当前用户")
        void getCurrentUser_ok() {
            User user = buildUser();
            when(userService.getById(1L)).thenReturn(user);
            when(userService.toUserResponse(user)).thenReturn(buildUserResponse());

            UserResponse resp = authService.getCurrentUser(1L);

            assertThat(resp.getUserId()).isEqualTo(1L);
        }
    }
}
package com.library.user.service;

import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.common.util.JwtUtil;
import com.library.user.dto.UserDTO.*;
import com.library.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        if ("禁用".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        userService.updateLastLoginTime(user.getUserId());

        String accessToken = JwtUtil.generateToken(
                user.getUserId(), 
                user.getUsername(), 
                user.getUserType()
        );
        
        String refreshToken = JwtUtil.generateRefreshToken(
                user.getUserId(), 
                user.getUsername(), 
                user.getUserType()
        );

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L);
        response.setUser(userService.toUserResponse(user));

        log.info("用户登录成功: userId={}, username={}", user.getUserId(), user.getUsername());
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        User user = userService.create(request);
        
        String accessToken = JwtUtil.generateToken(
                user.getUserId(), 
                user.getUsername(), 
                user.getUserType()
        );
        
        String refreshToken = JwtUtil.generateRefreshToken(
                user.getUserId(), 
                user.getUsername(), 
                user.getUserType()
        );

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L);
        response.setUser(userService.toUserResponse(user));

        log.info("用户注册成功: userId={}, username={}", user.getUserId(), user.getUsername());
        return response;
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!JwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token 无效或已过期");
        }

        Long userId = JwtUtil.getUserId(refreshToken);
        String username = JwtUtil.getUsername(refreshToken);
        String role = JwtUtil.getRole(refreshToken);

        String newAccessToken = JwtUtil.generateToken(userId, username, role);
        String newRefreshToken = JwtUtil.generateRefreshToken(userId, username, role);

        User user = userService.getById(userId);
        
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L);
        response.setUser(userService.toUserResponse(user));

        log.info("Token刷新成功: userId={}", userId);
        return response;
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userService.getById(userId);
        return userService.toUserResponse(user);
    }
}

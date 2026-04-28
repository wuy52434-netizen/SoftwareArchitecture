package com.library.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.common.util.JwtUtil;
import com.library.user.dto.UserDTO.*;
import com.library.user.entity.User;
import com.library.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public List<User> listAll() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .orderByDesc(User::getCreatedAt)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public User create(RegisterRequest request) {
        User existUser = getByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setUserType("reader");
        user.setStatus("正常");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);
        log.info("用户创建成功: username={}", user.getUsername());
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public User update(Long userId, UpdateRequest request) {
        User user = getById(userId);
        
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
        log.info("用户更新成功: userId={}", userId);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        User user = getById(userId);
        if ("admin".equals(user.getUserType())) {
            throw new BusinessException("不能删除管理员账户");
        }
        user.setDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户删除成功: userId={}", userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(Long userId) {
        User user = getById(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setUserType(user.getUserType());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());
        response.setLastLoginTime(user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : null);
        response.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return response;
    }
}

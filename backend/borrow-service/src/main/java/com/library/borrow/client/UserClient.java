package com.library.borrow.client;

import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/users/internal/{id}")
    Result<UserInfo> getUserById(@PathVariable Long id);

    @GetMapping("/api/users/current")
    Result<UserInfo> getCurrentUser(@RequestHeader("Authorization") String token);

    @PutMapping("/api/users/{id}/borrow-count")
    Result<Void> updateBorrowCount(@PathVariable Long id, @RequestParam Integer delta);

    @PutMapping("/api/users/{id}/status")
    Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam String status);

    class UserInfo {
        private Long id;
        private Long userId;
        private String username;
        private String realName;
        private String email;
        private String phone;
        private String status;
        private String role;
        private Integer activeBorrowCount;
        private Integer maxBorrowCount;

        public Long getId() { return id != null ? id : userId; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Integer getActiveBorrowCount() { return activeBorrowCount; }
        public void setActiveBorrowCount(Integer activeBorrowCount) { this.activeBorrowCount = activeBorrowCount; }
        public Integer getMaxBorrowCount() { return maxBorrowCount; }
        public void setMaxBorrowCount(Integer maxBorrowCount) { this.maxBorrowCount = maxBorrowCount; }
    }
}

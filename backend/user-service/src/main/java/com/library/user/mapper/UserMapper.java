package com.library.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM user WHERE user_id = #{userId} AND deleted = 0")
    User selectById(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND return_date IS NULL AND deleted = 0")
    int countActiveBorrows(@Param("userId") Long userId);
}

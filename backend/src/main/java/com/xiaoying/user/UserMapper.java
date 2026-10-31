package com.xiaoying.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO app_user (id, created_at) VALUES (#{id}, #{createdAt})")
    int insert(UserEntity user);

    @Select("SELECT id, created_at FROM app_user WHERE id = #{id}")
    UserEntity findById(@Param("id") String id);
}

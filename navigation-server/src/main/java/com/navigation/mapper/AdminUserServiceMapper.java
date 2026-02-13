package com.navigation.mapper;

import com.github.pagehelper.Page;
import com.navigation.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminUserServiceMapper {

    /**
     * 根据邮箱或昵称进行条件查询并分页展示
     */
    @Select("<script>" +
            "SELECT user_id, nick_name, email, gender, create_time, is_valid " +
            "FROM user " +
            "WHERE 1=1 " +
            "<if test='email != null and email != &quot;&quot;'> AND email LIKE CONCAT('%', #{email}, '%') </if>" +
            "<if test='nickName != null and nickName != &quot;&quot;'> AND nick_name LIKE CONCAT('%', #{nickName}, '%') </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Page<User> findUsersByCondition(@Param("email") String email, @Param("nickName") String nickName);

    /**
     * 查询所有用户信息并分页，按账号创建时间排序
     */
    @Select("SELECT user_id, nick_name, email, gender, create_time, is_valid " +
            "FROM user " +
            "ORDER BY create_time DESC")
    Page<User> findAllUsers();
    /**
     * 根据用户ID删除用户
     */
    @Delete("DELETE FROM user WHERE user_id = #{userId}")
    int deleteById(@Param("userId") Integer userId);
    /**
     * 根据用户ID更新用户的昵称、性别、年龄、是否有效状态、角色和更新时间
     */
    @Update("<script>" +
            "UPDATE user " +
            "<set>" +
            "  <if test='user.nickName != null'> nick_name = #{user.nickName}, </if>" +
            "  <if test='user.gender != null'> gender = #{user.gender}, </if>" +
            "  <if test='user.age != null'> age = #{user.age}, </if>" +
            "  <if test='user.isValid != null'> is_valid = #{user.isValid}, </if>" +
            "  <if test='user.role != null'> role = #{user.role}, </if>" +
            "  update_time = #{user.updateTime} " + // 总是更新修改时间
            "</set>" +
            "WHERE user_id = #{user.userId}" +
            "</script>")
    int updateUser(@Param("user") User user);

}
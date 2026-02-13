package com.navigation.mapper;

import com.navigation.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {


    /**
     * 新增账号
     * @param user
     * @return
     */
    @Insert("INSERT INTO user (nick_name, email, password, salt, age, gender, activation_time, create_time, is_valid, confirm_code, update_time, head, role) " +
            "VALUES (#{nickName}, #{email}, #{password}, #{salt}, #{age}, #{gender}, #{activationTime}, #{createTime}, #{isValid}, #{confirmCode}, #{updateTime}, #{head}, #{role})")
    int insertUser(User user);

    /**
     * 根据确认码查询用户
     * @param confirmCode
     * @return
     */
    @Select("SELECT email, activation_time FROM user WHERE confirm_code = #{confirmCode} AND is_valid = 0")
    User selectUserByConfirmCode(@Param("confirmCode") String confirmCode);

    /**
     * 根据确认码查询用户并修改状态值为 1（可用），只有在确认码有效且未激活的情况下才进行更新
     * @param confirmCode
     * @return
     */
    @Update("UPDATE user SET is_valid = 1 WHERE confirm_code = #{confirmCode} AND is_valid = 0")
    int updateUserByConfirmCode(@Param("confirmCode") String confirmCode);

    /**
     * 根据邮箱查询账户
     * 对于普通用户，is_valid 必须为 1；对于管理员账户无需激活状态的限制
     * @param email
     * @return
     */
    @Select("SELECT * FROM user WHERE email = #{email} AND (is_valid = 1 OR role = 'admin')")
    User selectUserByEmail(@Param("email") String email);


    /**
     * 根据邮箱查询账户
     * 不限制账户是否激活或角色
     * @param email
     * @return
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectUserByEmail1(@Param("email") String email);

    /**
     * 根据id查询账户
     * @param userId
     * @return
     */
    @Select("SELECT * FROM user WHERE user_id = #{userId}")
    User selectUserById(@Param("userId") Integer userId);

    /**
     * 更新用户个人信息（支持动态更新）
     * @param user 用户信息
     * @return 更新操作影响的行数
     */
    @Update("<script>" +
            "UPDATE user " +
            "<set>" +
            "<if test='nickName != null'>nick_name = #{nickName},</if>" +
            "<if test='age != null'>age = #{age},</if>" +
            "<if test='gender != null'>gender = #{gender},</if>" +
            "<if test='head != null'>head = #{head},</if>" +
            "update_time = #{updateTime} " +
            "</set>" +
            "WHERE user_id = #{userId}" +
            "</script>")
    int updateUserPersonalInfo(User user);

    /**
     * 修改用户密码
     * @param user 用户对象（包含 userId 和新密码）
     * @return 更新成功的记录数
     */
    @Update("UPDATE user SET password = #{password}, update_time = #{updateTime} WHERE user_id = #{userId}")
    int updateUserPassword(User user);

    @Update("UPDATE user " +
            "SET confirm_code = #{confirmCode}, " +
            "activation_time = #{activationTime}, " +
            "is_valid = #{isValid}, " +
            "password = #{password}, " +
            "salt = #{salt}, " +
            "update_time = #{updateTime} " +
            "WHERE email = #{email}")
    void updateUserForReRegister(User existingUser);

    @Select("SELECT nick_name, head FROM user WHERE user_id = #{userId}")
    User selectNickNameAndHeadById(Integer userId);


}
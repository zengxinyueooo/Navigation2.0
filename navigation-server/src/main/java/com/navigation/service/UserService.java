package com.navigation.service;

import com.navigation.dto.LoginDto;
import com.navigation.dto.RegisterDto;
import com.navigation.dto.UserUpdateDto;
import com.navigation.entity.User;
import com.navigation.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * 用户注册（仅适用于普通用户）
     * <p>
     * 注册成功后会发送激活邮件；管理员账号不支持此注册激活流程，需要提前在数据库中配置。
     * </p>
     *
     * @param registerDto 注册信息数据传输对象
     * @return 操作结果及附加信息，如注册成功、失败、邮箱已存在等
     */
    Map<String, Object> RegisterUser(RegisterDto registerDto);

    /**
     * 用户登录（支持普通用户和管理员）
     * <p>
     * 普通用户需经过注册激活且密码经过加密比对；<br/>
     * 管理员账号直接使用明文密码进行比对，无需激活。<br/>
     * 登录成功后返回 JWT 令牌和用户角色信息，用于后续接口验证。
     * </p>
     *
     * @param loginDto 登录信息数据传输对象
     * @return 操作结果及附加信息，如登录成功、失败、token、用户角色等
     */
    Map<String, Object> LoginUser(LoginDto loginDto);

    /**
     * 账号激活（仅适用于普通用户）
     * <p>
     * 根据激活码确认用户身份，并完成账户激活操作；如果激活码过期则需重新注册。
     * </p>
     *
     * @param confirmCode 激活码
     * @return 操作结果及附加信息，如激活成功、失败、链接失效等
     */
    Map<String, Object> activationAccount(String confirmCode);
    /**
     * 获取用户个人资料
     * <p>
     * 根据 JWT Token 获取当前用户的个人信息。
     * </p>
     *
     * @param token JWT 令牌
     * @return 用户资料
     */
    Map<String, Object> getUserProfile(String token);
    /**
     * 用户自己修改个人信息，包括昵称、年龄、性别、头像、密码
     * 自动更新修改操作时间
     * @param user 包含用户ID和要更新的字段（昵称、性别、年龄、头像、密码）
     * @return 是否更新成功
     */
    boolean updateUserPersonalInfo(User user);

    /**
     * 用户修改密码
     * @param userId 用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean updateUserPassword(Integer userId, String oldPassword, String newPassword);

}

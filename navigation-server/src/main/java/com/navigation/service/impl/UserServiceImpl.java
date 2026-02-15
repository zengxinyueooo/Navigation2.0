package com.navigation.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.navigation.dto.LoginDto;
import com.navigation.dto.RegisterDto;
import com.navigation.dto.UserUpdateDto;
import com.navigation.entity.User;
import com.navigation.exception.AccountExist;
import com.navigation.mapper.UserMapper;
import com.navigation.result.Result;
import com.navigation.service.MailService;
import com.navigation.service.UserService;
import com.navigation.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.xmlbeans.impl.piccolo.util.DuplicateKeyException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.navigation.constant.MessageConstant.USER_NICK_NAME_PREFIX;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private MailService mailService;

    private UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 注册账号
     */
    @Transactional
    public Map<String, Object> RegisterUser(RegisterDto registerDto) {
        Map<String, Object> resultMap = new HashMap<>();

        // 查询邮箱是否已存在
        User existingUser = userMapper.selectUserByEmail(registerDto.getEmail());

        // 雪花算法生成确认码
        String confirmCode = IdUtil.getSnowflake(1, 1).nextIdStr();

        // 生成盐值并加密密码
        String salt = RandomUtil.randomString(6);
        String md5Pwd = SecureUtil.md5(registerDto.getPassword() + salt);

        // 设置激活时间为24小时后
        LocalDateTime activationDeadline = LocalDateTime.now().plusDays(1);

        if (existingUser != null) {
            if (existingUser.getIsValid() == 1) {
                resultMap.put("status", "failure");
                resultMap.put("message", "邮箱已存在");
                return resultMap;
            }

            // 未激活则更新信息
            existingUser.setPassword(md5Pwd);
            existingUser.setSalt(salt);
            existingUser.setConfirmCode(confirmCode);
            existingUser.setActivationTime(activationDeadline);
            existingUser.setUpdateTime(LocalDateTime.now());

            userMapper.updateUserForReRegister(existingUser);

            String activationUrl = "http://47.96.179.70:8080/users/activation?confirmCode=" + confirmCode;
            System.out.println("重新发送激活链接：" + activationUrl);
            mailService.sendMailForActivationAccount(activationUrl, existingUser.getEmail());

            resultMap.put("status", "success");
            resultMap.put("message", "邮箱未激活，已重新发送激活邮件");
            return resultMap;
        }

        // 创建新用户对象
        User newUser = new User();
        newUser.setNickName(registerDto.getNickName());
        newUser.setEmail(registerDto.getEmail());
        newUser.setPassword(md5Pwd);
        newUser.setSalt(salt);
        newUser.setAge(registerDto.getAge());
        newUser.setGender(registerDto.getGender());
        newUser.setConfirmCode(confirmCode);
        newUser.setActivationTime(activationDeadline);

        // 根据角色判断是否激活
        if ("admin".equals(registerDto.getRole())) {
            newUser.setIsValid(1); // 管理员自动激活
            newUser.setRole("admin");
        } else {
            newUser.setIsValid(0); // 普通用户需激活
            newUser.setRole("user");
        }

        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        try {
            int result = userMapper.insertUser(newUser);
            if (result > 0) {
                if ("user".equals(newUser.getRole())) {
                    String activationUrl = "http://47.96.179.70:8080/users/activation?confirmCode=" + confirmCode;
                    System.out.println("注册成功，激活链接：" + activationUrl);
                    mailService.sendMailForActivationAccount(activationUrl, newUser.getEmail());
                    resultMap.put("status", "success");
                    resultMap.put("message", "注册成功，请查收激活邮件");
                } else {

                    resultMap.put("status", "success");
                    resultMap.put("message", "管理员注册成功");
                    System.out.println("注册时密码：" + registerDto.getPassword());
                    System.out.println("注册时盐值：" + salt);
                    System.out.println("加密后的密码：" + md5Pwd);
                }
            } else {
                resultMap.put("status", "failure");
                resultMap.put("message", "注册失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("status", "failure");
            resultMap.put("message", "数据库错误：" + e.getMessage());
        }

        return resultMap;
    }



    public Map<String, Object> LoginUser(LoginDto loginDto) {
        Map<String, Object> resultMap = new HashMap<>();

        // 查询账户
        User user = userMapper.selectUserByEmail1(loginDto.getEmail());
        if (user == null) {
            resultMap.put("code", 400);
            resultMap.put("message", "该账户不存在");
            return resultMap;
        }

        // 检查角色是否匹配
        if (!loginDto.getRole().equalsIgnoreCase(user.getRole())) {
            resultMap.put("code", 400);
            resultMap.put("message", "角色不匹配");
            return resultMap;
        }

        // 所有用户都进行加密密码比对
        String md5Pwd = SecureUtil.md5(loginDto.getPassword() + user.getSalt());

        if (!md5Pwd.equals(user.getPassword())) {


            System.out.println("登录时密码：" + loginDto.getPassword());
            System.out.println("登录时盐值：" + user.getSalt());
            System.out.println("登录时加密后的密码：" + md5Pwd);

            resultMap.put("code", 400);
            resultMap.put("message", "用户名或密码错误");
            return resultMap;
        }

        // 普通用户需检查激活状态
        if ("user".equalsIgnoreCase(user.getRole()) && user.getIsValid() == 0) {
            resultMap.put("code", 400);
            resultMap.put("message", "该账户未激活");
            return resultMap;
        }

        // 登录成功，生成 JWT 令牌
        String token = JwtUtils.generateToken(user.getUserId(), user.getRole());
        resultMap.put("code", 200);
        resultMap.put("message", "登录成功");
        resultMap.put("token", token);
        resultMap.put("role", user.getRole());
        return resultMap;
    }


    /**
     * 账号激活（仅适用于普通用户）
     */
    @RequestMapping(value = "/activation", method = RequestMethod.GET)
    public Map<String, Object> activationAccount(String confirmCode) {
        Map<String, Object> resultMap = new HashMap<>();
        if (confirmCode == null || confirmCode.isEmpty()) {
            resultMap.put("code", 400);
            resultMap.put("message", "激活码无效");
            return resultMap;
        }
        User user = userMapper.selectUserByConfirmCode(confirmCode);
        if (user == null) {
            resultMap.put("code", 400);
            resultMap.put("message", "用户未找到");
            return resultMap;
        }
        // 检查激活时间是否过期（这里假设激活时间为注册后24小时有效）
        boolean isExpired = LocalDateTime.now().isAfter(user.getActivationTime().plusHours(24));
        if (isExpired) {
            resultMap.put("code", 400);
            resultMap.put("message", "链接已失效，请重新注册");
            return resultMap;
        }
        // 更新用户激活状态
        int result = userMapper.updateUserByConfirmCode(confirmCode);
        if (result > 0) {
            resultMap.put("code", 200);
            resultMap.put("message", "激活成功");
        } else {
            resultMap.put("code", 400);
            resultMap.put("message", "激活失败，数据库更新错误");
        }
        return resultMap;
    }

    /**通过 userId获取用户信息
     *
     * @param token
     * @return
     */
    public Map<String, Object> getUserProfile(String token) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            // 获取用户 ID
            Integer userId = JwtUtils.getUserId(token);

            if (userId == null) {
                resultMap.put("status", "failure");
                resultMap.put("message", "无效的 Token");
                return resultMap;
            }

            // 通过 userId 获取用户信息
            User user = userMapper.selectUserById(userId);

            if (user != null) {
                resultMap.put("status", "success");
                resultMap.put("message", "获取成功");
                resultMap.put("user", user);
            } else {
                resultMap.put("status", "failure");
                resultMap.put("message", "用户未找到");
            }
        } catch (Exception e) {
            resultMap.put("status", "failure");
            resultMap.put("message", "获取用户信息失败");
            e.printStackTrace();
        }
        return resultMap;
    }
    /**
     * 用户自己修改个人信息
     * @param user 包含用户ID和要更新的字段（昵称、性别、年龄、头像）
     * @return 是否更新成功
     */
    @Override
    public boolean updateUserPersonalInfo(User user) {
        try {

            user.setUpdateTime(LocalDateTime.now());

            int result = userMapper.updateUserPersonalInfo(user);

            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *用户修改密码
     * @param userId 用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @return
     */
    @Override
    public boolean updateUserPassword(Integer userId, String oldPassword, String newPassword) {
        try {

            User existingUser = userMapper.selectUserById(userId);
            if (existingUser == null) {
                return false;
            }

            String salt = existingUser.getSalt();
            String oldEncrypted = SecureUtil.md5(oldPassword + salt);


            if (!oldEncrypted.equals(existingUser.getPassword())) {
                return false;
            }

            String newEncrypted = SecureUtil.md5(newPassword + salt);
            existingUser.setPassword(newEncrypted);
            existingUser.setUpdateTime(LocalDateTime.now());

            int result = userMapper.updateUserPassword(existingUser);
            return result > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }





}
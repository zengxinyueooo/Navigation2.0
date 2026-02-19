package com.navigation.controller.user;

import com.navigation.dto.LoginDto;
import com.navigation.dto.RegisterDto;
import com.navigation.entity.User;
import com.navigation.service.UserService;

import com.navigation.utils.JwtUtils;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.navigation.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/users")
@Api(tags = "用户账号管理")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录（支持管理员和普通用户，登录成功返回 JWT Token）
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "用户输入账号、密码和角色进行登录，角色为 user 或 admin")
    @ApiResponses({
            @ApiResponse(code = 1, message = "登录成功，返回 token", response = Result.class),
            @ApiResponse(code = 0, message = "登录失败，账号或密码错误")
    })
    public Result<Map<String, Object>> userLogin(
            @RequestBody @ApiParam(value = "登录请求数据", required = true) LoginDto loginDto) {
        Map<String, Object> result = userService.LoginUser(loginDto);
        Integer code = (Integer) result.get("code");

        if (code != null && code == 200) {
            // 登录成功，返回 token 和 role
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("token", result.get("token"));
            data.put("role", result.get("role"));
            return Result.success(data, (String) result.get("message"));
        } else {
            // 登录失败
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 用户注册（仅适用于普通用户）
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "用户输入必要信息进行注册，并在注册成功后提示用户前往邮箱激活账号")
    @ApiResponses({
            @ApiResponse(code = 1, message = "注册成功，请前往邮箱激活账号", response = Result.class),
            @ApiResponse(code = 0, message = "注册失败，邮箱已存在或其他错误")
    })
    public Result<String> registerUser(
            @RequestBody @ApiParam(value = "注册请求数据", required = true) RegisterDto registerDto) {
        Map<String, Object> result = userService.RegisterUser(registerDto);
        if (result == null || !"success".equals(result.get("status"))) {
            return Result.error(result != null ? (String) result.get("message") : "注册失败");
        } else {
            return Result.success(null, (String) result.get("message"));
        }
    }

    /**
     * 账号激活（仅适用于普通用户）
     */
    @GetMapping("/activation")
    @ApiOperation(value = "账号激活", notes = "用户点击邮件中的激活链接进行账号激活")
    @ApiImplicitParam(name = "confirmCode", value = "激活码", required = true, paramType = "query", dataType = "String")
    @ApiResponses({
            @ApiResponse(code = 1, message = "激活成功", response = Result.class),
            @ApiResponse(code = 0, message = "激活失败，激活码无效")
    })
    public Result<String> activationAccount(@RequestParam String confirmCode) {
        Map<String, Object> result = userService.activationAccount(confirmCode);
        Integer code = (Integer) result.get("code");

        if (code != null && code == 200) {
            return Result.success(null, (String) result.get("message"));
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 获取当前登录用户信息（通过 JWT Token）
     */
    @GetMapping("/profile")
    @ApiOperation(value = "获取当前登录用户信息", notes = "通过请求头携带的 JWT Token 获取当前用户信息")
    @ApiResponses({
            @ApiResponse(code = 1, message = "获取成功", response = Result.class),
            @ApiResponse(code = 0, message = "Token 无效或已过期")
    })
    public Result<User> getUserProfile(@RequestHeader("Authorization") String token) {
        try {
            // 解析 token 获取用户 ID
            Integer userId = JwtUtils.getUserId(token);

            if (userId == null) {
                return Result.error("Token 无效或已过期");
            }

            // 调用服务层获取用户信息
            Map<String, Object> userProfile = userService.getUserProfile(token);

            if ("success".equals(userProfile.get("status"))) {
                return Result.success((User) userProfile.get("user"), (String) userProfile.get("message"));
            } else {
                return Result.error("用户信息未找到");
            }
        } catch (Exception e) {
            System.out.println("异常信息：" + e.getMessage());
            e.printStackTrace();
            return Result.error("Token 无效或已过期");
        }
    }


    /**
     * 用户自己修改个人信息
     * @param user 包含用户ID和要更新的字段（昵称、性别、年龄、头像、密码）
     * @return 是否更新成功
     */
    @PutMapping("/update")
    @ApiOperation(value = "用户修改个人信息", notes = "用户修改自己的昵称、性别、年龄、头像、密码等信息")
    @ApiResponses({
            @ApiResponse(code = 1, message = "修改成功", response = Result.class),
            @ApiResponse(code = 0, message = "修改失败")
    })
    public Result<String> updateUserPersonalInfo(
            @RequestBody @ApiParam(value = "修改个人信息请求数据", required = true) User user,
            @RequestHeader("Authorization") String token) {

        try {
            // 解析 token 获取用户 ID
            Integer userId = JwtUtils.getUserId(token);  // 获取 Integer 类型的 userId

            if (userId == null) {
                return Result.error("Token 无效或已过期");
            }

            // 确保修改的是当前用户的个人信息
            if (userId == null || !userId.equals(user.getUserId())) {
                return Result.error("无法修改其他用户的信息");
            }

            // 调用服务层进行更新
            boolean isUpdated = userService.updateUserPersonalInfo(user);
            if (isUpdated) {
                return Result.success(null, "修改成功");
            } else {
                return Result.error("修改失败");
            }
        } catch (Exception e) {
            return Result.error("Token 无效或已过期");
        }
    }

    @PutMapping("/updatePassword")
    @ApiOperation(value = "修改用户密码", notes = "用户或管理员需要输入旧密码和新密码")
    public Result<?> updatePassword(
            @RequestHeader("Authorization") String token,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        try {
            Integer userId = JwtUtils.getUserId(token);
            String role = JwtUtils.getUserRole(token);

            if (userId == null || role == null) {
                return Result.error("Token 无效或已过期");
            }


            if (!"user".equals(role) && !"admin".equals(role)) {
                return Result.error("无权限操作，非法角色身份");
            }

            boolean isUpdated = userService.updateUserPassword(userId, oldPassword, newPassword);
            return isUpdated ? Result.success("密码修改成功") : Result.error("旧密码错误或修改失败");

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改过程中发生错误");
        }
    }



}
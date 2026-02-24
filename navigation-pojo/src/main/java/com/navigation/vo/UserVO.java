package com.navigation.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（不包含敏感信息）
 * 用于管理员查询用户列表等场景
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {

    private Integer userId; // 用户ID

    private String nickName; // 昵称

    private String email; // 用户邮箱地址

    private Integer age; // 用户年龄

    private String gender; // 用户性别（M: 男, F: 女）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 账号注册时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 账号修改时间

    private Integer isValid; // 账号是否有效（0：未激活，1：已激活）

    private String head; // 用户头像URL

    private String role; // 用户角色（user: 普通用户, admin: 管理员）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activationTime; // 账号激活失效时间（仅用于未激活账号）
}

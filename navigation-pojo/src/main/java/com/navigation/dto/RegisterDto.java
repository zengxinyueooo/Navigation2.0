package com.navigation.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户注册 DTO
 */
@Data
@ApiModel("用户注册请求参数")
public class RegisterDto {

    @ApiModelProperty(value = "昵称", required = true)
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    @ApiModelProperty(value = "邮箱", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ApiModelProperty(value = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能小于6位")
    private String password;

    @ApiModelProperty(value = "用户年龄", required = true)
    @NotNull(message = "年龄不能为空")
    private Integer age;

    @ApiModelProperty(value = "性别 (M: 男, F: 女)", required = true)
    @NotBlank(message = "性别不能为空")
    private String gender;

    private String role;

    private String confirmCode;  // 激活码
    private LocalDateTime activationTime;  // 激活时间
    private int isValid;  // 账户是否有效（0 表示无效，1 表示有效）
}
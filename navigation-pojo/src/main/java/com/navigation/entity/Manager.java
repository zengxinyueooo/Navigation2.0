package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

/**
 * 管理员实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("manager")
public class Manager {

    @TableId(value = "manager_id", type = IdType.AUTO)
    private Integer managerId; // 管理员ID

    @NotBlank(message = "管理员账号不能为空")
    private String managerName; // 管理员账号

    @NotBlank(message = "管理员密码不能为空")
    private String managerPassword; // 管理员密码

    @NotBlank(message = "角色不能为空")
    private String role; // 角色字段：'user' 代表普通用户，'admin' 代表管理员

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}
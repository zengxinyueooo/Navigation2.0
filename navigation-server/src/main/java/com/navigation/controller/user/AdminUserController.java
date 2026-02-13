package com.navigation.controller.user;

import com.github.pagehelper.PageInfo;
import com.navigation.entity.User;
import com.navigation.service.AdminUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@Api(tags = "管理员用户管理接口") // 说明这个控制器是用户管理相关接口
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 根据条件（邮箱或昵称）分页查询用户
     */
    @ApiOperation(
            value = "根据条件分页查询用户",
            notes = "根据邮箱或昵称进行条件查询，返回分页后的用户列表"
    )
    @GetMapping("/search")
    public PageInfo<User> searchUsers(
            @RequestParam(required = false)
            @ApiParam(value = "查询条件：用户邮箱", required = false) String email,

            @RequestParam(required = false)
            @ApiParam(value = "查询条件：用户昵称", required = false) String nickName,

            @RequestParam(defaultValue = "1")
            @ApiParam(value = "当前页码", defaultValue = "1") int pageNum,

            @RequestParam(defaultValue = "10")
            @ApiParam(value = "每页显示的条数", defaultValue = "10") int pageSize
    ) {
        return adminUserService.getUsersByCondition(pageNum, pageSize, email, nickName);
    }

    /**
     * 查询所有用户并分页，按账号创建时间排序
     */
    @ApiOperation(
            value = "查询所有用户分页列表",
            notes = "返回所有用户信息并分页展示，按照账号创建时间进行排序"
    )
    @GetMapping("/all")
    public PageInfo<User> getAllUsers(
            @RequestParam(defaultValue = "1")
            @ApiParam(value = "当前页码", defaultValue = "1") int pageNum,

            @RequestParam(defaultValue = "10")
            @ApiParam(value = "每页显示的条数", defaultValue = "10") int pageSize
    ) {
        return adminUserService.getAllUsers(pageNum, pageSize);
    }
    /**
     * 根据用户ID删除用户
     */
    @ApiOperation(value = "根据ID删除用户", notes = "根据用户ID删除指定的用户")
    @DeleteMapping("/delete/{userId}")
    public boolean deleteUser(
            @PathVariable
            @ApiParam(value = "用户ID", required = true) Integer userId
    ) {
        return adminUserService.deleteUserById(userId);
    }
    /**
     * 根据用户ID修改用户的昵称、性别、年龄、是否有效状态和角色
     * 自动更新修改时间
     */
    @ApiOperation(value = "根据ID修改用户信息", notes = "根据用户ID修改用户的昵称、性别、年龄、是否有效状态和角色")
    @PutMapping("/update")
    public boolean updateUserInfo(
            @RequestBody
            @ApiParam(value = "用户信息对象", required = true) User user
    ) {
        return adminUserService.updateUserInfo(user);
    }
}
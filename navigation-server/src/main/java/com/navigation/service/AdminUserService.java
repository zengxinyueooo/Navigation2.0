package com.navigation.service;

import com.github.pagehelper.PageInfo;
import com.navigation.entity.User;

import java.util.List;

public interface AdminUserService {

    /**
     * 根据条件（邮箱或昵称）进行分页查询
     */
    PageInfo<User> getUsersByCondition(int pageNum, int pageSize, String email, String nickName);

    /**
     * 查询所有用户并分页，按账号创建时间排序
     */
    PageInfo<User> getAllUsers(int pageNum, int pageSize);
    /**
     * 根据用户ID删除用户
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUserById(Integer userId);
    /**
     * 根据用户ID修改用户的昵称、性别、年龄、是否有效状态和角色
     * @param user 包含用户ID和要更新的字段（昵称、性别、年龄、是否有效状态、角色）
     * @return 是否更新成功
     */
    boolean updateUserInfo(User user);

}
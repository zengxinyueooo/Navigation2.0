package com.navigation.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.navigation.entity.User;
import com.navigation.mapper.AdminUserServiceMapper;
import com.navigation.result.PageResult;
import com.navigation.service.AdminUserService;
import com.navigation.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserControllerServiceImpl implements AdminUserService {

    @Autowired
    private AdminUserServiceMapper adminUserServiceMapper;

    /**
     * 根据条件（邮箱或昵称）进行分页查询
     */
    @Override
    public PageResult<UserVO> getUsersByCondition(int pageNum, int pageSize, String email, String nickName) {
        // 启动分页功能
        PageHelper.startPage(pageNum, pageSize);

        // 查询符合条件的用户
        List<User> userList = adminUserServiceMapper.findUsersByCondition(email, nickName);
        Page<User> page = (Page<User>) userList;

        // 转换为 UserVO（过滤敏感信息）
        List<UserVO> userVOList = userList.stream().map(this::convertToUserVO).collect(Collectors.toList());

        // 使用简化的 PageResult 封装分页数据
        return new PageResult<>(page.getTotal(), userVOList);
    }

    /**
     * 查询所有用户并分页，按账号创建时间排序
     */
    @Override
    public PageResult<UserVO> getAllUsers(int pageNum, int pageSize) {
        // 启动分页功能
        PageHelper.startPage(pageNum, pageSize);

        // 查询所有用户并按账号创建时间排序
        List<User> userList = adminUserServiceMapper.findAllUsers();
        Page<User> page = (Page<User>) userList;

        // 转换为 UserVO（过滤敏感信息）
        List<UserVO> userVOList = userList.stream().map(this::convertToUserVO).collect(Collectors.toList());

        // 使用简化的 PageResult 封装分页数据
        return new PageResult<>(page.getTotal(), userVOList);
    }

    /**
     * 将 User 转换为 UserVO，过滤敏感信息
     */
    private UserVO convertToUserVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        // password, salt, confirmCode 不会被复制，因为 UserVO 中没有这些字段
        return userVO;
    }
    /**
     * 根据用户ID删除用户
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteUserById(Integer userId) {
        try {
            // 删除用户，假设 deleteById 是 Mapper 中的删除方法
            int result = adminUserServiceMapper.deleteById(userId);
            return result > 0;  // 返回 true 表示删除成功，返回 false 表示删除失败
        } catch (Exception e) {
            e.printStackTrace();
            return false;  // 出现异常时返回删除失败
        }
    }
    /**
     * 根据用户ID修改用户的昵称、性别、年龄、是否有效状态和角色
     * 自动更新时间戳
     * @param user 包含用户ID和要更新的字段（昵称、性别、年龄、是否有效状态、角色）
     * @return 是否更新成功
     */
    @Override
    public boolean updateUserInfo(User user) {
        try {
            // 设置更新时间为当前时间
            user.setUpdateTime(LocalDateTime.now());

            // 调用 Mapper 的更新方法
            int result = adminUserServiceMapper.updateUser(user);

            // 如果更新了至少一条记录，返回 true
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常信息
            return false; // 发生异常时返回 false
        }
    }
}

package com.navigation.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.navigation.entity.UserRoute;
import com.navigation.mapper.UserRouteMapper;
import com.navigation.service.UserRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class UserRouteServiceImpl implements UserRouteService {

    @Autowired
    private UserRouteMapper routeMapper;

    @Override
    public void saveRoute(UserRoute route) {
        // 设置创建时间
        route.setCreateTime(new Timestamp(System.currentTimeMillis()));
        route.setId(null);
        routeMapper.insert(route);  // 插入数据
    }
    @Override
    public UserRoute getRouteById(Long routeId) {
        return routeMapper.findById(routeId); // 查询方法可以在 Mapper 中实现
    }
    @Override
    public void deleteRoute(Long routeId) {
        routeMapper.deleteById(routeId);
    }
    @Override
    public PageInfo<UserRoute> getUserRoutesPaged(Integer userId, int pageNo, int pageSize) {
        // 启动分页
        PageHelper.startPage(pageNo, pageSize);

        // 执行查询
        List<UserRoute> routes = routeMapper.findByUserIdPaged(userId);

        // 返回分页后的结果，使用 PageInfo 包装查询结果
        return new PageInfo<>(routes);  // 返回 PageInfo<UserRoute>
    }

    @Override
    public List<UserRoute> searchUserRoutes(Integer userId, String keyword, Timestamp startTime, Timestamp endTime, int page, int size) {
        // 使用 PageHelper 开始分页
        PageHelper.startPage(page, size);  // PageHelper 分页处理
        return routeMapper.searchRoutesPaged(userId, keyword, startTime, endTime);
    }
}
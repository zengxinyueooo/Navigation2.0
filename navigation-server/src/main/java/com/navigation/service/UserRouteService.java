package com.navigation.service;

import com.github.pagehelper.PageInfo;
import com.navigation.entity.UserRoute;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
@Service
public interface UserRouteService {
    void saveRoute(UserRoute route);
    void deleteRoute(Long routeId);
    /**
     * 根据路线 ID 获取用户路线
     * @param routeId 路线ID
     * @return 用户路线对象
     */
    UserRoute getRouteById(Long routeId);
    PageInfo<UserRoute> getUserRoutesPaged(Integer userId, int pageNo, int PageSize);
    List<UserRoute> searchUserRoutes(Integer userId, String keyword, Timestamp startTime, Timestamp endTime, int page, int size);
}

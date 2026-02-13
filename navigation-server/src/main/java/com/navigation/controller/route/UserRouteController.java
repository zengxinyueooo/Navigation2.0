package com.navigation.controller.route;

import com.github.pagehelper.PageInfo;
import com.navigation.entity.UserRoute;
import com.navigation.service.UserRouteService;
import com.navigation.utils.JwtUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user-routes")
@Api(tags = "路线规划相关接口")  // 用于标记该控制器类属于路线规划相关的接口
public class UserRouteController {

    @Autowired
    private UserRouteService userRouteService;

    /**
     * 从请求头中获取 Token，解析出用户 ID
     * @param token 请求头中的 JWT Token
     * @return 用户 ID
     */
    private Integer getUserIdFromToken(String token) {
        return JwtUtils.getUserId(token);  // 通过 JwtUtils 工具类解析出用户 ID
    }

    /**
     * 保存用户的路线
     * @param token 用户的 JWT Token
     * @param route 用户路线对象，包含起点、终点等信息
     * @return 返回保存操作结果的消息
     */
    @PostMapping
    @ApiOperation(value = "保存用户的路线", notes = "根据传入的路线信息保存用户路线，必须提供有效的 Token")
    public String saveRoute(
            @ApiParam(value = "用户的 JWT Token", required = true) @RequestHeader("Authorization") String token,
            @ApiParam(value = "用户路线对象", required = true) @RequestBody UserRoute route) {

        Integer userId = getUserIdFromToken(token);
        if (userId == null) {
            return "无效的Token，无法验证用户身份";
        }

        route.setUserId(userId); // 设置当前用户的 ID
        route.setId(null);  // 强制清空 id 字段，确保数据库自动生成
        userRouteService.saveRoute(route);
        return "保存成功";
    }


    /**
     * 删除用户的路线
     * @param token 用户的 JWT Token
     * @param routeId 要删除的路线的 ID
     * @return 返回删除操作结果的消息
     */
    @DeleteMapping("/{routeId}")
    @ApiOperation(value = "删除用户的路线", notes = "根据路线 ID 删除用户的路线，删除前会验证用户身份")
    public String deleteRoute(
            @ApiParam(value = "用户的 JWT Token", required = true) @RequestHeader("Authorization") String token,
            @ApiParam(value = "要删除的路线 ID", required = true) @PathVariable Long routeId) {

        Integer userId = getUserIdFromToken(token);
        if (userId == null) {
            return "无效的Token，无法验证用户身份";  // Token 无效时，返回身份验证失败
        }

        // 验证该路线是否属于当前用户
        UserRoute route = userRouteService.getRouteById(routeId);
        if (route == null || !route.getUserId().equals(userId)) {
            return "该路线不存在或您没有权限删除此路线";  // 如果路线不存在或不属于该用户，返回错误信息
        }

        userRouteService.deleteRoute(routeId);  // 调用服务层删除路线
        return "删除成功";  // 返回删除成功信息
    }

    /**
     * 分页获取用户的所有路线
     * @param token 用户的 JWT Token
     * @param userId 用户的 ID
     * @param pageNo 页码，默认值为 1
     * @param pageSize 每页的路线数量，默认值为 10
     * @return 返回用户的路线列表，包含分页信息
     */
    @GetMapping("/{userId}/list")
    @ApiOperation(value = "分页获取用户的路线", notes = "根据用户 ID 分页获取用户的所有路线")
    public PageInfo<UserRoute> getRoutesByPage(
            @ApiParam(value = "用户的 JWT Token", required = true) @RequestHeader("Authorization") String token,
            @ApiParam(value = "用户的 ID", required = true) @PathVariable Integer userId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int pageNo,
            @ApiParam(value = "每页的路线数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {

        // 验证 Token 用户和请求中的用户是否匹配
        Integer tokenUserId = getUserIdFromToken(token);
        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new SecurityException("无效的Token，无法验证用户身份");
        }

        // 调用 Service 层获取分页后的路线列表
        return userRouteService.getUserRoutesPaged(userId, pageNo, pageSize);
    }


    /**
     * 综合搜索并分页查询用户的路线
     * @param token 用户的 JWT Token
     * @param userId 用户的 ID
     * @param keyword 关键词，用于模糊匹配起点或终点
     * @param startTime 起始时间，用于筛选创建时间
     * @param endTime 截止时间，用于筛选创建时间
     * @param page 页码，默认值为 1
     * @param size 每页的路线数量，默认值为 10
     * @return 返回符合条件的用户路线列表
     */
    @GetMapping("/{userId}/search")
    @ApiOperation(value = "综合搜索用户路线", notes = "根据关键词、时间范围等条件搜索用户的路线")
    public List<UserRoute> searchRoutes(
            @ApiParam(value = "用户的 JWT Token", required = true) @RequestHeader("Authorization") String token,
            @ApiParam(value = "用户的 ID", required = true) @PathVariable Integer userId,
            @ApiParam(value = "搜索关键词", required = false) @RequestParam(required = false) String keyword,
            @ApiParam(value = "起始时间", required = false) @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Timestamp startTime,
            @ApiParam(value = "结束时间", required = false) @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Timestamp endTime,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页的路线数量", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {

        Integer tokenUserId = getUserIdFromToken(token);
        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new SecurityException("无效的Token，无法验证用户身份");  // Token 无效或用户不匹配时，抛出异常
        }

        // 调用服务层进行综合搜索并返回分页查询结果
        return userRouteService.searchUserRoutes(userId, keyword, startTime, endTime, page, size);
    }
}
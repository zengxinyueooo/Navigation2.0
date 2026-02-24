package com.navigation.interceptor;

import com.navigation.context.BaseContext;
import com.navigation.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT认证拦截器
 * 自动从请求头中解析token，提取userId并存入BaseContext
 * 对于需要登录的接口（如聊天、会话管理），强制要求token
 */
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        log.debug("[JwtAuthInterceptor] 请求URI: {}", uri);

        // 从请求头中获取Token（支持两种格式）
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀
        }

        // 尝试解析token
        if (token != null && !token.isEmpty()) {
            try {
                Integer userId = JwtUtils.getUserId(token);
                if (userId != null) {
                    BaseContext.saveUserId(userId);
                    log.debug("[JwtAuthInterceptor] Token解析成功 | userId={}", userId);
                    return true;
                }
            } catch (Exception e) {
                log.error("[JwtAuthInterceptor] Token解析失败 | token={} | error={}", token, e.getMessage());
            }
        }

        // 判断是否是需要登录的接口
        if (isAuthRequired(uri)) {
            log.warn("[JwtAuthInterceptor] 未登录访问受保护接口 | uri={}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
            return false;
        }

        // 非必须登录的接口，允许通过（但没有userId）
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后，清理ThreadLocal，避免内存泄漏
        BaseContext.remove();
    }

    /**
     * 判断是否是需要登录的接口
     */
    private boolean isAuthRequired(String uri) {
        // 聊天相关接口必须登录
        if (uri.startsWith("/AI/chat") || uri.startsWith("/AI/session")) {
            return true;
        }

        // 预订相关接口必须登录
        if (uri.startsWith("/ticket_reservation") || uri.startsWith("/scenic_reservation")) {
            return true;
        }

        // 收藏相关接口必须登录
        if (uri.startsWith("/scenic/favorite") || uri.startsWith("/hotel/favorite")) {
            return true;
        }

        // 路线规划接口必须登录
        if (uri.startsWith("/api/user-routes")) {
            return true;
        }

        // 用户个人信息相关接口必须登录
        if (uri.equals("/users/profile") || uri.equals("/users/update") || uri.equals("/users/updatePassword")) {
            return true;
        }

        return false;
    }
}

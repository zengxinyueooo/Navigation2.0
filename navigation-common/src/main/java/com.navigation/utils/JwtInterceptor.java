package com.navigation.utils;

import com.navigation.context.BaseContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;



public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取 Token
        String token = request.getHeader("Authorization");
        if (token != null) {
            try {
                // 调用 JwtUtils 解析 Token 获取用户 ID
                Integer userId = JwtUtils.getUserId(token);
                if (userId != null) {
                    // 将用户 ID 存入 BaseContext
                    BaseContext.saveUserId(userId);
                }
            } catch (Exception e) {
                // 处理解析 Token 时的异常
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 请求处理完成后，可进行一些清理操作
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后，移除 BaseContext 中的用户 ID，避免内存泄漏
        BaseContext.remove();
    }
}
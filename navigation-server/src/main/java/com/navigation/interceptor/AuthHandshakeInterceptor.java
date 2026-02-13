package com.navigation.interceptor;

import com.navigation.utils.JwtUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 从请求参数中获取token
        String token = extractTokenFromRequest(request);
        if (token != null && JwtUtils.validateToken(token)) {
            Integer userId = JwtUtils.getUserId(token);
            attributes.put("userId", userId); // 把用户ID存起来
            return true; // 验证通过
        }
        return false; // 验证失败，拒绝连接
    }

    // 提取 Token 方法（从请求头中获取 Token）
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // 从请求头中获取 Authorization 字段
        String token = request.getHeaders().getFirst("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7); // 去掉 "Bearer " 前缀，返回纯 Token
        }

        return null; // 如果没有找到 Token，则返回 null
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
/*具体验证过程：
        提取token：从URL参数中获取用户登录凭证
        验证token：使用JWT工具验证token是否有效
        解析用户ID：从token中提取用户身份信息
        存储用户信息：把用户ID存起来，后续处理使用*/

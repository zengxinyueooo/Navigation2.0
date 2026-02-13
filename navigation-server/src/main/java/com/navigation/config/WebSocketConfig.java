package com.navigation.config;

import com.navigation.handler.OrderWebSocketHandler;
import com.navigation.interceptor.AuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket //告诉Spring我们要启用WebSocket功能
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new OrderWebSocketHandler(), "/ws/orders") //注册一个处理器，指定访问路径是/ws/orders
                .setAllowedOrigins("*") //允许所有域名访问
                .addInterceptors(new AuthHandshakeInterceptor()); //添加拦截器，用于用户身份验证
    }
    //前端通过这个URL连接：ws://你的域名/ws/orders?token=用户token
}
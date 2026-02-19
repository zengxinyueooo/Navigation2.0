package com.navigation.config;

import com.navigation.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 配置JWT拦截器，自动从token中解析userId并存入BaseContext
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor())
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/error",                // 错误页面
                        "/swagger-ui/**",        // Swagger文档
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/v3/api-docs",
                        "/webjars/**",
                        "/doc.html",             // knife4j文档
                        "/favicon.ico"
                );
    }
}

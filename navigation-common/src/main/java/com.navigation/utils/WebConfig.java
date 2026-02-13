package com.navigation.utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns(
                        "/scenic/favorite/**",  // 拦截景区收藏相关路径
                        "/hotel/favorite/**",    // 拦截酒店收藏相关路径
                        "/scenic_reservation/**",
                        "/ticket_reservation/**",
                        "/scenic/addComments/**",
                        "/scenic/scenicMark/**",
                        "/hotel/hotelMark/**"
                );
        // 无需 excludePathPatterns，其他路径默认不拦截
    }
}

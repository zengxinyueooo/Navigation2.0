package com.navigation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@MapperScan("com.navigation.mapper")
@SpringBootApplication
@EnableConfigurationProperties
// 排除 langchain4j 的扫描包或类
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "dev\\.langchain4j\\.service\\.spring\\..*")
})
public class  NavigationApplication {

    public static void main(String[] args) {
// 关键：禁用 langchain4j 的自动扫描
        System.setProperty("langchain4j.ai.services.auto-scan.enabled", "false");
        SpringApplication.run(NavigationApplication.class,args);

    }
}


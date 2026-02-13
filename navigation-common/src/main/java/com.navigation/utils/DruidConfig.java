package com.navigation.utils;

import com.alibaba.druid.pool.DruidDataSource;

public class DruidConfig {
    public static DruidDataSource getDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://47.96.179.70:3306/navigation_system");
        dataSource.setUsername("root");
        dataSource.setPassword("@oPHc701eA0dcQEATe2Ae");

        dataSource.setMaxWait(30000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000); // 60秒检测一次
        dataSource.setMaxActive(10); // 最大连接数
        dataSource.setMinIdle(5); // 最小空闲连接数

        return dataSource;
    }
}
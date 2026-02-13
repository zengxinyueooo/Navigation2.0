package com.navigation.service.impl;

import com.navigation.mapper.WeatherConditionsMapper;
import com.navigation.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class WeatherServiceImpl implements WeatherService {

    @Autowired
    private WeatherConditionsMapper weatherConditionsMapper;

    /**
     * 根据天气现象返回景点开放状态
     * @param weatherCondition 天气现象
     * @return 开放状态（0 不开放, 1 开放）
     */
    public int getParkStatusByWeather(String weatherCondition) {
        // 查询天气现象对应的状态
        Integer status = weatherConditionsMapper.getStatusByWeatherCondition(weatherCondition);
        if (status == null) {
            return 0; // 如果天气不在表中，默认不开放
        }
        return status;
    }
}
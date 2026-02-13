package com.navigation.service;

import org.springframework.stereotype.Service;

@Service

public interface WeatherService {
    public int getParkStatusByWeather(String weatherCondition);
}
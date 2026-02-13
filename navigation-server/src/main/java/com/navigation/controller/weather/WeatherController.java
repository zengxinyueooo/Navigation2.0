package com.navigation.controller.weather;

import com.navigation.service.WeatherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
@Api(tags = "天气相关接口")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;  // 自动装配 WeatherService

    /**
     * 根据天气条件获取景点开放状态
     *
     * @param weatherCondition 天气条件（如 "晴"、"雷阵雨" 等）
     * @return 景点开放状态（0 为不开放，1 为开放）
     */
    @ApiOperation(value = "根据天气条件查询景点开放状态", notes = "根据天气情况返回景点是否开放")
    @GetMapping("/status")
    public int getParkStatusByWeather(
            @RequestParam
            @ApiParam(value = "天气条件", required = true) String weatherCondition) {
        return weatherService.getParkStatusByWeather(weatherCondition);
    }
}
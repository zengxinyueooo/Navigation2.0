package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("weather_conditions")
public class WeatherCondition {

    private Integer id;  // 自增ID
    private String weatherCondition;  // 天气现象描述
    private Integer status;  // 是否开放，0表示不开放，1表示开放

}
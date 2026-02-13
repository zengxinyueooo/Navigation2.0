package com.navigation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WeatherConditionsMapper {

    @Select("SELECT status FROM weather_conditions WHERE weather_condition = #{weatherCondition}")
    Integer getStatusByWeatherCondition(@Param("weatherCondition") String weatherCondition);
}
package com.navigation.dto;

import lombok.Data;

@Data
public class HotelFavoriteDTO {
    // 假设这些是 HotelFavorite 类中的字段
    private Integer id;
    private Integer userId;
    private Integer hotelId;
    private String hotelName;
    // 新增的字段
    private String cover;
    private String hotelDescription;
}
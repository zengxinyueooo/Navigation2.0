package com.navigation.service;

import com.navigation.entity.HotelFavorite;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import java.util.List;

public interface HotelFavoriteService {

    /**
     * 保存酒店收藏
     * @param hotelId 酒店ID
     * @return 操作结果
     */
    Result<Void> save( Integer hotelId);

    /**
     * 取消酒店收藏
     * @param hotelId 酒店ID
     * @return 操作结果
     */
    Result<Void> cancel(Integer hotelId);


   // Result<List<HotelFavorite>> getHotelFavoritesByUserId(Integer userId);


    Result<Integer> isHotelFavorite(Integer hotelId);



    Result<HotelFavorite> getHotelFavoriteInfoByUserIdAndHotelId(Integer hotelId);

    PageResult queryPageByUserId(Integer page, Integer pageSize);

    Result<List<HotelFavorite>> getHotelFavoriteInfoByUserIdAndHotelName(String hotelName);
}
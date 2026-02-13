package com.navigation.service;

import com.navigation.entity.ScenicFavorite;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import io.swagger.models.auth.In;

import java.util.List;

public interface ScenicFavoriteService {

    Result<Void> save(Integer scenicId);


    Result<Void> cancel(Integer scenicId);


    // 根据用户ID和景点ID查询是否已收藏
    Result<Integer> isScenicFavorite(Integer scenicId);


    // 根据用户ID查询该用户的所有收藏景点记录
   // Result<List<ScenicFavorite>> getScenicFavoritesByUserId(Integer userId);


    Result<ScenicFavorite> getFavoriteInfoByUserIdAndScenicId(Integer scenicId);


    PageResult queryPageByUserId(Integer page, Integer pageSize);

    Result<List<ScenicFavorite>> getFavoriteInfoByUserIdAndScenicName(String name);
}
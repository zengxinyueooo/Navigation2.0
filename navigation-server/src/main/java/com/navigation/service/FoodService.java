package com.navigation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.navigation.entity.Food;
import com.navigation.entity.Food;
import com.navigation.result.PageResult;
import com.navigation.result.Result;


import java.util.List;

public interface FoodService extends IService<Food> {

    Result<Void> saveFood(Food food);

    Result<Void> update(Food food);

    Result<Void> batchDelete(List<Integer> ids);

    PageResult queryFood(Integer pageNum, Integer pageSize);

    Result<Food> queryFoodById(Integer id);

    Result<List<Food>> queryFoodByRegionId(Integer id);

    Result<List<Food>> findByRegion(String region);
}

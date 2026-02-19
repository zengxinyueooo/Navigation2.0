package com.navigation.controller.food;

import com.navigation.entity.Food;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/food")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @PostMapping("/save")
    public Result<Void> saveFood(@RequestBody Food food){

        return foodService.saveFood(food);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Food food){

        return foodService.update(food);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){

        return foodService.batchDelete(ids);
    }

    @GetMapping("/query")
    public Result<PageResult> queryFood(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        PageResult pageResult = foodService.queryFood(page,pageSize);
        return Result.success(pageResult);
    }

    @GetMapping("/queryByRegionId")
    public Result<List<Food>> queryFoodByRegionId(Integer id){
        return foodService.queryFoodByRegionId(id);
    }

    @GetMapping("/queryById")
    public Result<Food> queryFoodById(Integer id){
        return foodService.queryFoodById(id);
    }

}
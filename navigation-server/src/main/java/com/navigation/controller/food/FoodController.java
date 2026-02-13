package com.navigation.controller.food;

import com.navigation.entity.Food;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.FoodService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/food")
public class FoodController {

    @Resource
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
    public PageResult queryFood(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return foodService.queryFood(page,pageSize);
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
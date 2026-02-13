package com.navigation.controller.region;

import com.navigation.entity.Region;

import com.navigation.entity.Scenic;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.RegionService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/region")
public class RegionController {

    @Resource
    private RegionService regionService;

    @PostMapping("/save")
    public Result<Void> saveRegion(@RequestBody Region region){

        return regionService.saveRegion(region);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Region region){

        return regionService.update(region);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){

        return regionService.batchDelete(ids);
    }

    @GetMapping("/query")
    public PageResult queryRegion(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return regionService.queryRegion(page,pageSize);
    }

    @GetMapping("/queryById")
    public Result<Region> queryRegionById(Integer id){
        return regionService.queryRegionById(id);
    }

    @GetMapping("/queryByName")
    public Result<List<Region>> queryScenicByName(@RequestParam String name){

        return regionService.queryScenicByName(name);
    }

}
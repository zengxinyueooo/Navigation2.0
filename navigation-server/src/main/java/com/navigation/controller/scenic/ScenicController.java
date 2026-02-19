package com.navigation.controller.scenic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navigation.dto.ScenicQueryDto;
import com.navigation.entity.Comment;
import com.navigation.entity.Scenic;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import com.navigation.service.ScenicService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/scenic")
public class ScenicController {

    @Autowired
    private ScenicService scenicService;

    @PostMapping("/save")
    public Result<Void> saveScenic(@RequestBody Scenic scenic){

        return scenicService.saveScenic(scenic);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Scenic scenic){
        return scenicService.update(scenic);
    }
    @PostMapping("/addComments/{scenicId}")
    public Result<Void> addComments(@PathVariable Integer scenicId, @RequestBody Comment comments){
        return scenicService.addComments(scenicId, comments);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){
        return scenicService.batchDelete(ids);
    }

    @GetMapping("/query")
    public Result<PageResult> queryScenic(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize) {
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return Result.success(scenicService.queryScenic(page, pageSize));
    }

    @GetMapping("/query2")
    public Result<PageResult> queryScenic2(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize) {
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return Result.success(scenicService.queryScenic2(page, pageSize));
    }

    @GetMapping("/queryComments")
    public Result<PageResult> queryScenicComments(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize,
                                  @RequestParam Integer scenicId){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return Result.success(scenicService.queryScenicComments(page,pageSize, scenicId));
    }
    @GetMapping("/queryPageByRegionId")
    public Result<PageResult> queryPageByRegionId(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize,
                                  @RequestParam Integer regionId){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return Result.success(scenicService.queryPageByRegionId(page,pageSize,regionId));
    }



    @GetMapping("/queryById")
    public Result<Scenic> queryScenicById(@RequestParam Integer id){

        return scenicService.queryScenicById(id);
    }

    @GetMapping("/queryByName")
    public Result<List<Scenic>> queryScenicByName(@RequestParam String name){

        return scenicService.queryScenicByName(name);
    }

    @PostMapping("/queryTotalPeople")
    public Result<Void> queryTotalPeople(@RequestParam Integer id){

        return scenicService.queryTotalPeople(id);
    }

    @PostMapping("/scenicMark")
    public Result<Void> scenicMark(@RequestParam Integer id, @RequestParam Integer mark){
        return scenicService.scenicMark(id,mark);
    }

    @GetMapping("/IsCongested")
    public Result<Boolean> IsCongested(@RequestParam Integer id){
        return scenicService.IsCongested(id);
    }

    @GetMapping("/queryTotalPeopleByScenicId")
    public Result<Integer> queryTotalPeopleByScenicId(@RequestParam Integer id){
        return scenicService.queryTotalPeopleByScenicId(id);
    }






}

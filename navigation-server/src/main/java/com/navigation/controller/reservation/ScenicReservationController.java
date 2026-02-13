package com.navigation.controller.reservation;

import com.navigation.entity.Scenic;
import com.navigation.entity.ScenicReservation;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.ScenicReservationService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/scenic_reservation")
public class ScenicReservationController {

    @Resource
    private ScenicReservationService scenicReservationService;

    @PostMapping("/save")
    public Result<Void> saveScenicReservation(@RequestBody ScenicReservation scenicReservation){

        return scenicReservationService.saveScenicReservation(scenicReservation);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody ScenicReservation scenicReservation){
        return scenicReservationService.update(scenicReservation);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){
        return scenicReservationService.batchDelete(ids);
    }

    @GetMapping("/query")
    public PageResult queryScenicReservation(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return scenicReservationService.queryScenicReservation(page,pageSize);
    }

    @GetMapping("/queryTop10")
    public PageResult queryScenicReservationTop10(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return scenicReservationService.queryScenicReservationTop10(page,pageSize);
    }


    /*@GetMapping("/queryById")
    public Result<ScenicReservation> queryScenicReservationById(Integer id){
        return scenicReservationService.queryScenicReservationById(id);
    }*/





}

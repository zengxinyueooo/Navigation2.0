package com.navigation.controller.hotel;

import com.navigation.entity.Hotel;
import com.navigation.entity.Hotel;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.HotelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Resource
    private HotelService hotelService;

    @PostMapping("/save")
    public Result<Void> saveHotel(@RequestBody Hotel hotel){

        return hotelService.saveHotel(hotel);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Hotel hotel){

        return hotelService.update(hotel);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){

        return hotelService.batchDelete(ids);
    }

    @GetMapping("/query")
    public PageResult queryHotel(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return hotelService.queryHotel(page,pageSize);
    }

    @GetMapping("/query2")
    public PageResult queryHotel2(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return hotelService.queryHotel2(page,pageSize);
    }


    @GetMapping("/queryById")
    public Result<Hotel> queryHotelById(@RequestParam Integer id){
        return hotelService.queryHotelById(id);
    }

    @GetMapping("/queryByRegionId")
    public Result<List<Hotel>> queryHotelByRegionId(@RequestParam Integer id){
        return hotelService.queryHotelByRegionId(id);}

    @GetMapping("/queryByName")
    public Result<List<Hotel>> queryHotelByName(@RequestParam String name){
        return hotelService.queryHotelByName(name);
    }

    @PostMapping("/hotelMark")
    public Result<Void> hotelMark(@RequestParam Integer id,  @RequestParam Integer mark){
        return hotelService.hotelMark(id,mark);
    }
}
package com.navigation.controller.favorite;

import com.navigation.entity.HotelFavorite;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.HotelFavoriteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/hotel/favorite")
public class HotelFavoriteController {

    @Resource
    private HotelFavoriteService hotelFavoriteService;


    /**
     * 保存酒店收藏
     * @param hotelId 酒店ID
     * @return 操作结果
     */
    @PostMapping("/save/{hotelId}")
    public Result<Void> save(@PathVariable Integer hotelId) {
        return hotelFavoriteService.save(hotelId);
    }

    /**
     * 取消酒店收藏
     * @param hotelId 酒店ID
     * @return 操作结果
     */
    @PostMapping("/cancel/{hotelId}")
    public Result<Void> cancel(@PathVariable Integer hotelId) {
        return hotelFavoriteService.cancel(hotelId);
    }


    /*@GetMapping("/user/{userId}")
    public Result<List<HotelFavorite>> getHotelFavoritesByUserId(@PathVariable Integer userId) {
        return hotelFavoriteService.getHotelFavoritesByUserId(userId);
    }*/

     //根据用户ID和酒店ID查询是否已收藏
     @GetMapping("/isFavorite/{hotelId}")
    public Result<Integer> isHotelFavorite(@PathVariable Integer hotelId) {
        return hotelFavoriteService.isHotelFavorite(hotelId);
    }


    @GetMapping("/info/{hotelId}")
    public Result<HotelFavorite> getHotelFavoriteInfo(@PathVariable Integer hotelId) {
        return hotelFavoriteService.getHotelFavoriteInfoByUserIdAndHotelId(hotelId);
    }

    @GetMapping("/info/name/{hotelName}")
    public Result<List<HotelFavorite>> getHotelFavoriteInfoByName(@PathVariable String hotelName) {
        return hotelFavoriteService.getHotelFavoriteInfoByUserIdAndHotelName(hotelName);
    }

    @GetMapping("/queryPage")
    public PageResult queryPageByUserId(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return hotelFavoriteService.queryPageByUserId(page,pageSize);
    }
}
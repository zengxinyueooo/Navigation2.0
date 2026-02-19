package com.navigation.controller.favorite;

import com.navigation.entity.ScenicFavorite;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.ScenicFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;



import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/scenic/favorite")
public class ScenicFavoriteController {

    @Autowired
    private ScenicFavoriteService scenicFavoriteService;


    @PostMapping("/save/{scenicId}")
    public Result<Void> save( @PathVariable Integer scenicId) {
        return scenicFavoriteService.save(scenicId);
    }


    @PostMapping("/cancel/{scenicId}")
    public Result<Void> cancelFavoriteScenic(@PathVariable Integer scenicId) {
        return scenicFavoriteService.cancel(scenicId);
    }

    // 根据用户ID和景点ID查询是否已收藏
    @GetMapping("/isFavorite/{scenicId}")
    public Result<Integer> isScenicFavorite(@PathVariable Integer scenicId) {
        return scenicFavoriteService.isScenicFavorite(scenicId);
    }

    // 根据用户ID和景点ID查询该景点收藏信息
    @GetMapping("/info/{scenicId}")
    public Result<ScenicFavorite> getFavoriteInfo(@PathVariable Integer scenicId) {
        return scenicFavoriteService.getFavoriteInfoByUserIdAndScenicId(scenicId);
    }

    // 根据用户ID和景点名称查询该景点收藏信息
    @GetMapping("/info/name/{scenicName}")
    public Result<List<ScenicFavorite>> getFavoriteInfoByName(@PathVariable String scenicName) {
        return scenicFavoriteService.getFavoriteInfoByUserIdAndScenicName(scenicName);
    }

    @GetMapping("/queryPage")
    public Result<PageResult> queryPageByUserId(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return Result.success(scenicFavoriteService.queryPageByUserId(page,pageSize));
    }

    // 根据用户ID查询该用户的所有收藏景点记录
    /*@GetMapping("/user/{userId}")
    public Result<List<ScenicFavorite>> getScenicFavoritesByUserId(@PathVariable Integer userId) {
        return scenicFavoriteService.getScenicFavoritesByUserId(userId);
    }*/
}
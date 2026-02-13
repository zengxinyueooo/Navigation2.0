package com.navigation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.navigation.entity.Region;
import com.navigation.entity.Scenic;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import java.util.List;

public interface RegionService extends IService<Region> {

    Result<Void> saveRegion(Region region);

    Result<Void> update(Region region);

    Result<Void> batchDelete(List<Integer> ids);

    PageResult queryRegion(Integer pageNum, Integer pageSize);


    Result<Region> queryRegionById(Integer id);

    Result<List<Region>> queryScenicByName(String name);
}
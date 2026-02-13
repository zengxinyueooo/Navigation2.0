package com.navigation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.navigation.dto.ScenicQueryDto;
import com.navigation.entity.Comment;
import com.navigation.entity.Scenic;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import java.util.List;

public interface ScenicService extends IService<Scenic> {

    Result<Void> saveScenic(Scenic scenic);

    Result<Void> update(Scenic scenic);

    Result<Void> batchDelete(List<Integer> ids);

    PageResult queryScenic(Integer pageNum, Integer pageSize);


    Result<Scenic> queryScenicById(Integer id);

    Result<Void> addComments(Integer scenicId, Comment comments);

    PageResult queryPageByRegionId(Integer page, Integer pageSize, Integer regionId);

    Result<List<Scenic>> queryPageByRegionName(Integer page, Integer pageSize, String regionName);

    Result<Void> queryTotalPeople(Integer id);

    Result<Void> scenicMark(Integer id, Integer mark);

    Result<Boolean> IsCongested(Integer id);

    Result<Integer> queryTotalPeopleByScenicId(Integer id);

    Result<List<Scenic>> queryScenicByName(String name);

    PageResult queryScenicComments(Integer page, Integer pageSize,Integer scenicId);


    PageResult queryScenic2(Integer page, Integer pageSize);
}

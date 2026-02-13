package com.navigation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.navigation.entity.Scenic;
import com.navigation.entity.ScenicReservation;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import java.util.List;

public interface ScenicReservationService extends IService<ScenicReservation> {

    Result<Void> saveScenicReservation(ScenicReservation scenicReservation);

    Result<Void> update(ScenicReservation scenicReservation);

    Result<Void> batchDelete(List<Integer> ids);

    PageResult queryScenicReservation(Integer pageNum, Integer pageSize);

    PageResult queryScenicReservationTop10(Integer page, Integer pageSize);


    //Result<ScenicReservation> queryScenicReservationById(Integer id);
}

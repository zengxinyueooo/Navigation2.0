package com.navigation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.navigation.entity.TicketReservation;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import java.util.List;

public interface TicketReservationService extends IService<TicketReservation> {

    Result<Integer> saveTicketReservation(TicketReservation ticketReservation);

    Result<Void> update(TicketReservation ticketReservation);

    Result<Void> batchDelete(List<Integer> ids);

    PageResult queryTicketReservation(Integer pageNum, Integer pageSize);


    //Result<TicketReservation> queryTicketReservationById(Integer id);

    Result<Void> confirmPurchase(Integer reservationId);
}

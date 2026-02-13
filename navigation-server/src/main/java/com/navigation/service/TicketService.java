package com.navigation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.navigation.entity.Ticket;
import com.navigation.result.PageResult;
import com.navigation.result.Result;

import java.util.List;

public interface TicketService extends IService<Ticket> {

    Result<Void> saveTicket(Ticket ticket);

    Result<Void> update(Ticket ticket);

    Result<Void> batchDelete(List<Integer> ids);

    Result<List<Ticket>> queryByScenicId(Integer id);

    Result<List<Ticket>> queryByScenicName(String name);


    Result<Ticket> queryTicketById(Integer ticketId);

    PageResult queryTicket(Integer page, Integer pageSize);
}
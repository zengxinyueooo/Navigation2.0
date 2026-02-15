package com.navigation.controller.ticket;

import com.navigation.entity.Region;
import com.navigation.entity.Ticket;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.RegionService;
import com.navigation.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/save")
    public Result<Void> saveRegion(@RequestBody Ticket ticket){

        return ticketService.saveTicket(ticket);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Ticket ticket){

        return ticketService.update(ticket);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){

        return ticketService.batchDelete(ids);
    }

    //根据景点id查询门票
    @GetMapping("/queryByScenicId")
    public Result<List<Ticket>> queryTicketByScenicId(Integer id){

        return ticketService.queryByScenicId(id);
    }
    @GetMapping("/queryById")
    public Result<Ticket> queryTicketById(Integer id){
        return ticketService.queryTicketById(id);
    }

    @GetMapping("/query")
    public PageResult queryTicket(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return ticketService.queryTicket(page,pageSize);
    }

}
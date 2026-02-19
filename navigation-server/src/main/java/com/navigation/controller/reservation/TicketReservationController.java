package com.navigation.controller.reservation;

import com.navigation.entity.TicketReservation;
import com.navigation.result.PageResult;
import com.navigation.result.Result;
import com.navigation.service.TicketReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/ticket_reservation")
public class TicketReservationController {

    @Autowired
    private TicketReservationService ticketReservationService;

    @PostMapping("/save")
    public Result<Integer> saveTicketReservation(@RequestBody TicketReservation ticketReservation){

        return ticketReservationService.saveTicketReservation(ticketReservation);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody TicketReservation ticketReservation){
        return ticketReservationService.update(ticketReservation);
    }

    @DeleteMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestParam List<Integer> ids){
        return ticketReservationService.batchDelete(ids);
    }

    @GetMapping("/query")
    public Result<PageResult> queryTicketReservation(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "5") Integer pageSize){
        //两个参数分别指：从第几页开始查，每页的个数有多少
        return Result.success(ticketReservationService.queryTicketReservation(page,pageSize));
    }

    /*@GetMapping("/queryById")
    public Result<TicketReservation> queryTicketReservationById(Integer id){
        return ticketReservationService.queryTicketReservationById(id);
    }*/

    @PostMapping("/confirmPurchase/{reservationId}")
    public Result<Void> confirmPurchase(@PathVariable Integer reservationId) {
        return ticketReservationService.confirmPurchase(reservationId);

    }





}

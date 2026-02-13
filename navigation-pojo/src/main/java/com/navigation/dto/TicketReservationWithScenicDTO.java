package com.navigation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketReservationWithScenicDTO {
    // TicketReservation 类原有的字段
    private Integer reservationId; // 预定ID
    private Integer userId; // 用户ID
    private Integer ticketId; // 门票ID
    private Integer quantity; // 预定的数量
    private BigDecimal totalPrice; // 总金额
    private java.time.LocalDateTime reservationTime; // 门票要预定的时间点


    // 新增的景点相关字段
    private Integer scenicId;
    private String scenicCover;
    private String scenicName;
    private String scenicLocateDescription;
}
package com.navigation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScenicReservationDTO {
    private Integer scenicId;      // 景点ID
    private String scenicName;
    private Long totalPeople;     // 预约总人数
    private String scenicCover;  //封面
}
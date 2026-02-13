package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票务预定实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ticket_reservation")
public class TicketReservation {

    @TableId(value = "reservation_id", type = IdType.AUTO)
    private Integer reservationId; // 预定ID

    @NotNull(message = "用户Id不能为空")
    private Integer userId; // 用户ID

    @NotNull(message = "门票Id不能为空")
    private Integer ticketId; // 门票ID

    @NotNull(message = "预订数量不能为空")
    private Integer quantity; // 预定的数量

    @NotNull(message = "预定时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime reservationTime; // 门票要预定的时间点

    private Integer status; // 预定状态，0表示未支付

    private BigDecimal totalPrice; // 总金额

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy年MM月dd日 HH时mm分", timezone = "GMT+8")
    private LocalDateTime updateTime; // 修改时间
}